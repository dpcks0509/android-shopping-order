package woowacourse.shopping.ui.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import woowacourse.shopping.R
import woowacourse.shopping.databinding.FragmentCartBinding
import woowacourse.shopping.ui.detail.DetailActivity
import woowacourse.shopping.ui.order.adapter.cart.CartAdapter
import woowacourse.shopping.ui.order.adapter.cart.ShoppingCartViewItem.CartViewItem
import woowacourse.shopping.ui.order.viewmodel.OrderViewModel
import woowacourse.shopping.ui.state.UiState

class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding: FragmentCartBinding
        get() = _binding!!

    private lateinit var adapter: CartAdapter
    private val viewModel by activityViewModels<OrderViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()
        setUpDataBinding()
        observeViewmodel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpAdapter() {
        adapter = CartAdapter(viewModel)
        binding.rvCart.adapter = adapter
    }

    private fun setUpDataBinding() {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }

    private fun observeViewmodel() {
        viewModel.cartUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> showData(state.data)
                is UiState.Loading -> showData(emptyList())
                is UiState.Error ->
                    showError(
                        state.exception.message ?: getString(R.string.unknown_error),
                    )
            }
        }
        viewModel.navigateToDetail.observe(viewLifecycleOwner) { navigateToDetail ->
            navigateToDetail.getContentIfNotHandled()?.let { productId ->
                navigateToDetail(productId)
            }
        }

        viewModel.notifyDeletion.observe(viewLifecycleOwner) { notifyDeletion ->
            notifyDeletion.getContentIfNotHandled()?.let {
                alertDeletion()
            }
        }

        viewModel.notifyCanNotOrder.observe(viewLifecycleOwner) { notifyCanNotOrder ->
            notifyCanNotOrder.getContentIfNotHandled()?.let {
                alertCanNotOrder()
            }
        }
    }

    private fun showData(cartViewItems: List<CartViewItem>) {
        adapter.submitCartViewItems(cartViewItems.toList())
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDetail(productId: Int) {
        startActivity(DetailActivity.createIntent(requireContext(), productId))
    }

    private fun alertDeletion() {
        Toast.makeText(requireContext(), DELETE_ITEM_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    private fun alertCanNotOrder() {
        Toast.makeText(requireContext(), CAN_NOT_ORDER_MESSAGE, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val DELETE_ITEM_MESSAGE = "장바구니에서 상품을 삭제했습니다!"
        private const val CAN_NOT_ORDER_MESSAGE = "최소 1개 이상의 상품을 주문해주세요!"

        fun newInstance(): Fragment {
            return CartFragment()
        }
    }
}
