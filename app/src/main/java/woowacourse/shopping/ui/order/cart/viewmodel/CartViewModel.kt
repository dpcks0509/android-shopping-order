package woowacourse.shopping.ui.order.cart.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import woowacourse.shopping.domain.model.CartItem
import woowacourse.shopping.domain.model.Product
import woowacourse.shopping.domain.repository.CartRepository
import woowacourse.shopping.ui.event.Event
import woowacourse.shopping.ui.order.cart.action.CartNavigationActions
import woowacourse.shopping.ui.order.cart.action.CartNotifyingActions
import woowacourse.shopping.ui.order.cart.adapter.ShoppingCartViewItem.CartViewItem
import woowacourse.shopping.ui.order.cart.listener.CartClickListener
import woowacourse.shopping.ui.order.viewmodel.OrderViewModel
import woowacourse.shopping.ui.state.UiState

class CartViewModel(
    private val cartRepository: CartRepository,
    private val orderViewModel: OrderViewModel,
) : ViewModel(), CartClickListener {
    private val _cartUiState =
        MutableLiveData<UiState<List<CartViewItem>>>(UiState.Loading)
    val cartUiState: LiveData<UiState<List<CartViewItem>>>
        get() = _cartUiState

    val isCartEmpty: LiveData<Boolean>
        get() =
            orderViewModel.cartViewItems.map { cartViewItemsValue ->
                cartViewItemsValue.isEmpty()
            }

    private val _cartNavigationActions = MutableLiveData<Event<CartNavigationActions>>()
    val cartNavigationActions: LiveData<Event<CartNavigationActions>>
        get() = _cartNavigationActions

    private val _cartNotifyingActions = MutableLiveData<Event<CartNotifyingActions>>()
    val cartNotifyingActions: LiveData<Event<CartNotifyingActions>>
        get() = _cartNotifyingActions

    init {
        Handler(Looper.getMainLooper()).postDelayed({
            loadCartViewItems()
        }, 1000)
    }

    fun updateCartUiState() {
        _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
    }

    private fun loadCartViewItems() {
        runCatching {
            val cartTotalQuantity = cartRepository.getCartTotalQuantity().getOrNull() ?: 0
            cartRepository.getCartItems(0, cartTotalQuantity, OrderViewModel.DESCENDING_SORT_ORDER)
        }.onSuccess {
            val cartViewItems = it.getOrNull()?.map(::CartViewItem) ?: return
            orderViewModel.updateCartViewItems(cartViewItems)
            _cartUiState.value = UiState.Success(cartViewItems)
        }.onFailure {
            _cartUiState.value = UiState.Error(it)
        }
    }

    override fun onCheckBoxClick(cartItemId: Int) {
        orderViewModel.onCheckBoxClick(cartItemId)

        _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
    }

    override fun onCartItemClick(productId: Int) {
        _cartNavigationActions.value = Event(CartNavigationActions.NavigateToDetail(productId))
    }

    override fun onDeleteButtonClick(cartItemId: Int) {
        orderViewModel.onDeleteButtonClick(cartItemId)

        _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
        _cartNotifyingActions.value = Event(CartNotifyingActions.NotifyCartItemDeleted)
    }

    override fun onPlusButtonClick(product: Product) {
        var cartItemId = -1
        runCatching {
            cartItemId = cartRepository.addCartItem(product.productId, 1).getOrNull() ?: return
        }.onSuccess {
            val updatedCartItem = CartViewItem(CartItem(cartItemId, 1, product))
            val newCartViewItems =
                orderViewModel.cartViewItems.value?.plus(updatedCartItem) ?: return
            orderViewModel.updateCartViewItems(newCartViewItems)
            _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
        }
    }

    override fun onQuantityPlusButtonClick(productId: Int) {
        orderViewModel.onQuantityPlusButtonClick(productId)

        _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
    }

    override fun onQuantityMinusButtonClick(productId: Int) {
        val updatedCartItem = orderViewModel.getCartViewItemByProductId(productId) ?: return
        if (updatedCartItem.cartItem.quantity > 1) {
            orderViewModel.onQuantityMinusButtonClick(productId)

            _cartUiState.value = UiState.Success(orderViewModel.cartViewItems.value ?: emptyList())
        }
    }
}