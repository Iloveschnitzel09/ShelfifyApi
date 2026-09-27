package de.schnitzel.shelfifyapi.controller

import de.schnitzel.shelfifyapi.repositories.EanMappingRepository
import de.schnitzel.shelfifyapi.repositories.ProductRepository
import de.schnitzel.shelfifyapi.repositories.UserRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
}