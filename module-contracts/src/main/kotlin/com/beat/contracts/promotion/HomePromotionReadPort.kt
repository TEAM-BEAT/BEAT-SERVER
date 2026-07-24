package com.beat.contracts.promotion

import com.beat.contracts.promotion.readmodel.HomePromotionReadModel

interface HomePromotionReadPort {
    fun findAllOrdered(): List<HomePromotionReadModel>
}
