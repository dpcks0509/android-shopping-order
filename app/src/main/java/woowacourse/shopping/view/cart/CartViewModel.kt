package woowacourse.shopping.view.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import woowacourse.shopping.data.model.CartItem2
import woowacourse.shopping.domain.model.CartItem
import woowacourse.shopping.domain.repository.CartRepository
import woowacourse.shopping.domain.repository.CartRepository2
import woowacourse.shopping.util.Event
import woowacourse.shopping.view.state.UIState

class CartViewModel(private val cartRepository: CartRepository2) :
    ViewModel(),
    CartItemClickListener,
    QuantityClickListener {
    private var lastPage: Int = DEFAULT_PAGE

    private val _cartUiState = MutableLiveData<UIState<List<CartItem2>>>(UIState.Loading)
    val cartUiState: LiveData<UIState<List<CartItem2>>>
        get() = _cartUiState

    private val _currentPage = MutableLiveData(DEFAULT_PAGE)
    val currentPage: LiveData<Int>
        get() = _currentPage

    val isFirstPage: LiveData<Boolean> =
        currentPage.map { currentPageValue ->
            currentPageValue == DEFAULT_PAGE
        }

    val isLastPage: LiveData<Boolean> =
        currentPage.map { currentPageValue ->
            currentPageValue == lastPage
        }

    val isEmpty: LiveData<Boolean> =
        cartUiState.map { state ->
            when (state) {
                is UIState.Success -> state.data.isEmpty()
                else -> true
            }
        }
    private val _isPageControlButtonVisible = MutableLiveData(false)
    val isPageControlButtonVisible: LiveData<Boolean> = _isPageControlButtonVisible

    private val _navigateToDetail = MutableLiveData<Event<Int>>()
    val navigateToDetail: LiveData<Event<Int>>
        get() = _navigateToDetail

    private val _notifyDeletion = MutableLiveData<Event<Boolean>>()
    val notifyDeletion: LiveData<Event<Boolean>>
        get() = _notifyDeletion

    private val _isBackButtonClicked = MutableLiveData<Event<Boolean>>()
    val isBackButtonClicked: LiveData<Event<Boolean>>
        get() = _isBackButtonClicked

    private val _updatedCartItem = MutableLiveData<CartItem2>()
    val updatedCartItem: LiveData<CartItem2>
        get() = _updatedCartItem

    private lateinit var cartItems: List<CartItem2>

    init {
        loadPage(_currentPage.value ?: DEFAULT_PAGE)
    }

    private fun updatePageControlVisibility(totalItems: Int) {
        _isPageControlButtonVisible.value = totalItems > PAGE_SIZE
    }

    fun loadPage(page: Int) {
        val totalItems = cartRepository.getCartTotalQuantity().getOrNull()?.quantity ?: 0
        lastPage = (totalItems - PAGE_STEP) / PAGE_SIZE
        _currentPage.value = page.coerceIn(DEFAULT_PAGE, lastPage)

        loadCartItems()
        updatePageControlVisibility(totalItems)
    }

    fun loadCartItems() {
        try {
            cartItems =
                cartRepository.getCartItems(currentPage.value ?: DEFAULT_PAGE, PAGE_SIZE, "asc")
                    .getOrNull()?.cartItems ?: emptyList()
//                cartRepository.findAllPagedItems(currentPage.value ?: DEFAULT_PAGE, PAGE_SIZE)
            _cartUiState.value = UIState.Success(cartItems)
        } catch (e: Exception) {
            _cartUiState.value = UIState.Error(e)
        }
    }

    fun loadNextPage() {
        val nextPage = (currentPage.value ?: DEFAULT_PAGE) + PAGE_STEP
        loadPage(nextPage)
    }

    fun loadPreviousPage() {
        val prevPage = (currentPage.value ?: DEFAULT_PAGE) - PAGE_STEP
        loadPage(prevPage)
    }

    fun deleteItem(itemId: Int) {
        cartRepository.deleteCartItem(itemId)
        cartItems = cartItems.filter { it.cartItemId != itemId }
        loadPage(currentPage.value ?: DEFAULT_PAGE)
    }

    override fun onCartItemClick(productId: Int) {
        _navigateToDetail.value = Event(productId)
    }

    override fun onDeleteButtonClick(itemId: Int) {
        deleteItem(itemId)
        _notifyDeletion.value = Event(true)
    }

    override fun onBackButtonClick() {
        _isBackButtonClicked.value = Event(true)
    }

    override fun onQuantityPlusButtonClick(productId: Int) {
        var cartItem = cartItems.first { it.product.id == productId }
        cartItem = cartItem.plusQuantity()
        cartRepository.updateCartItem(productId, cartItem.quantity)
        _updatedCartItem.value = cartItem
    }

    override fun onQuantityMinusButtonClick(productId: Int) {
        var cartItem = cartItems.first { it.product.id == productId }
        cartItem = cartItem.minusQuantity()
        cartRepository.updateCartItem(productId, cartItem.quantity)
        _updatedCartItem.value = cartItem
    }

    companion object {
        const val PAGE_SIZE = 5
        private const val DEFAULT_PAGE = 0
        private const val PAGE_STEP = 1
    }
}
