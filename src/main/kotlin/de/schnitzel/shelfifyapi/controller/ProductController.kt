package de.schnitzel.shelfifyapi.controller

import de.schnitzel.shelfifyapi.entities.EanMappingEntity
import de.schnitzel.shelfifyapi.entities.ProductEntity
import de.schnitzel.shelfifyapi.repositories.EanMappingRepository
import de.schnitzel.shelfifyapi.repositories.ProductRepository
import de.schnitzel.shelfifyapi.repositories.UserRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class ProductResponseDto(val name: String, val menge: Int, val ablaufdatum: LocalDate)
@RestController
class ProductController(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val eanMappingRepository: EanMappingRepository
) {

    @GetMapping("/products")
    fun getProducts(
        @AuthenticationPrincipal userId: Int,
        @RequestParam(required = false, defaultValue = "-1") days: Int
    ): List<ProductResponseDto> {
        val user = userRepository.findById(userId).orElseThrow { Exception("User nicht gefunden") }
        val datagroup = user.datagroup

        val products = if (days < 0) {
            productRepository.findByDatagroup(datagroup)
        } else {
            val cutoffDate = LocalDate.now().plusDays(days.toLong())
            productRepository.findByDatagroupAndAblaufdatumBefore(datagroup, cutoffDate)
        }

        val eanDataMap = HashMap<String, String>()

        eanMappingRepository.findAll().forEach { eanDataMap[it.ean] = it.productName }

        eanMappingRepository.findAllByDatagroup(datagroup).forEach { eanDataMap[it.ean] = it.productName }

        return products.map { product ->
            val productName = eanDataMap[product.ean] ?: "Unbekanntes Produkt"
            ProductResponseDto(
                name = productName,
                menge = product.menge,
                ablaufdatum = product.ablaufdatum,
            )
        }.sortedWith(compareBy<ProductResponseDto> { it.name }.thenBy { it.ablaufdatum })
    }

    @GetMapping("/lookupProductName")
    fun lookupProductName(
        @AuthenticationPrincipal userId: Int,
        @RequestParam ean: String
    ): ResponseEntity<String> {
        val user = userRepository.findById(userId).orElseThrow { Exception("User nicht gefunden") }
        val datagroup = user.datagroup

        try {
            val productName = eanMappingRepository.findByEanAndDatagroup(ean, datagroup)?.productName
            if (productName != null) {
                return ResponseEntity.ok(productName)
            }

            val globalProductName = eanMappingRepository.findByEan(ean)?.productName
            if(globalProductName != null) {
                return ResponseEntity.ok(globalProductName)
            }

            return fetchAndStoreProductNameFromApi(ean)

        } catch (_: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun fetchAndStoreProductNameFromApi(ean: String) : ResponseEntity<String> {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$ean.json")
            val connection =  url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            when (connection.responseCode) {
                200 -> {}
                404 -> { return ResponseEntity.status(HttpStatus.NOT_FOUND).build() }
                else -> { return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build() }
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            val productName = ObjectMapper()
                .readTree(response)
                .path("product")
                .path("product_name")
                .asString()

            if (!productName.isNullOrEmpty()) {
                val newEntry = EanMappingEntity().apply {
                    this.ean = ean
                    this.productName = productName
                    this.datagroup = null
                }
                eanMappingRepository.save(newEntry)
                return ResponseEntity.ok(productName)
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        } catch (_: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/addEan")
    fun addEan(
        @AuthenticationPrincipal userId: Int,
        @RequestParam ean: String,
        @RequestParam productName: String
    ) : ResponseEntity<String> {
        val datagroup = userRepository.findById(userId).orElseThrow { Exception("User nicht gefunden") }.datagroup
        val existing = eanMappingRepository.findByEanAndDatagroup(ean, datagroup)
            ?: eanMappingRepository.findByEanAndDatagroupIsNull(ean)
            ?: eanMappingRepository.findByProductNameAndDatagroup(productName, datagroup)
            ?: eanMappingRepository.findByProductNameAndDatagroupIsNull(productName)

        if (existing != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        val newMapping = EanMappingEntity(
            ean = ean,
            productName = productName,
            datagroup = datagroup
        )

        return try {
            eanMappingRepository.save(newMapping)
            ResponseEntity.ok().build()
        } catch (_: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/addProduct")
    fun addProduct(
        @AuthenticationPrincipal userId: Int,
        @RequestParam ean: String,
        @RequestParam quantity: Int,
        @RequestParam("ablaufdatum") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) expirationDate: LocalDate
    ) : ResponseEntity<String> {
        val datagroup = userRepository.findById(userId).orElseThrow { Exception("User nicht gefunden") }.datagroup

        val exist = productRepository.findByEanAndDatagroupAndAblaufdatum(ean, datagroup, expirationDate)
        if (exist.isNotEmpty()) {
            val product = exist[0]
            product.menge += quantity
            productRepository.save(product)
            return ResponseEntity.ok().build()
        }

        val product = ProductEntity(
            ean = ean,
            menge = quantity,
            ablaufdatum = expirationDate,
            datagroup = datagroup
        )
        productRepository.save(product)
        return ResponseEntity.ok().build()
    }

    @Transactional
    @DeleteMapping("/removeProduct")
    fun removeProduct(
        @AuthenticationPrincipal userId: Int,
        @RequestParam ean: String,
        @RequestParam(defaultValue = "1") quantity: Int
    ): ResponseEntity<String> {
        if (quantity < 1) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()

        val datagroup = userRepository.findById(userId).orElseThrow { Exception("User nicht gefunden") }.datagroup

        // Sortiert nach Ablaufdatum: ältestes zuerst
        val products = productRepository.findByEanAndDatagroupOrderByAblaufdatumAsc(ean, datagroup)
        if (products.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        var remaining = quantity
        for (product in products) {
            if (remaining <= 0) break

            if (product.menge > remaining) {
                product.menge -= remaining
                productRepository.save(product)
                remaining = 0
            } else {
                remaining -= product.menge
                productRepository.delete(product)
            }
        }

        return ResponseEntity.ok().build()
    }
}