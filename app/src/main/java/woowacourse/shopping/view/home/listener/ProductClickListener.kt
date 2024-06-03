package woowacourse.shopping.view.home.listener

import woowacourse.shopping.data.model.Product

interface ProductClickListener {
    fun onProductClick(productId: Int)

    fun onPlusButtonClick(product: Product)
}