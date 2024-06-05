package woowacourse.shopping.ui.order.listener

import woowacourse.shopping.domain.model.Product

interface RecommendClickListener {
    fun onProductClick(productId: Int)

    fun onPlusButtonClick(product: Product)
}