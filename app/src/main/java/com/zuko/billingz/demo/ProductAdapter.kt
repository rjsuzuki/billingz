package com.zuko.billingz.demo

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.zuko.billingz.R
import com.zuko.billingz.databinding.ListItemProductBinding
import com.zuko.billingz.lib.products.Product

/**
 * @author rjsuzuki
 */
class ProductAdapter(
    /**
     * Provide a list of the products you created from your Google Play account
     */
    private val list: MutableList<Product>
): RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ListItemProductBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ListItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = list[position]
        holder.binding.productTitle.text = item.sku ?: ""
        holder.binding.productDescription.text = item.description ?: ""
        holder.binding.productBuyBtn.setOnClickListener {
            //todo vm.productToPurchaseLD.value = item
            (it as LottieAnimationView).playAnimation()
        }
        holder.binding.productEditIb.setOnClickListener {
            Toast.makeText(it.context, "TODO", Toast.LENGTH_SHORT).show()
        }

        holder.binding.productRemoveIb.setOnClickListener {
            val dialog = AlertDialog.Builder(it.context, R.style.StudioDialog).create()
            dialog.setTitle(it.resources.getString(R.string.confirm_deletion))
            dialog.setMessage("${item.sku}?")
            dialog.setIcon(ResourcesCompat.getDrawable(it.context.resources, R.drawable.ic_baseline_delete_24, it.context.theme))
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, it.resources.getString(R.string.no)) { d, _ -> d?.cancel() }
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, it.resources.getString(R.string.yes)) { d, _ ->
                //todo vm.productRepository.delete(item) //db
                list.remove(item) //local
                notifyItemRemoved(position)
                d.dismiss()
            }
            dialog.show()
        }

        holder.binding.root.animation = AnimationUtils.loadAnimation(holder.binding.root.context, R.anim.product_item_anim)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * Add [Product] to list
     */
    fun addProduct(product: Product) {
        val previousSize = list.size
        list.add(product)
        notifyItemInserted(previousSize)
    }

    /**
     * Remove [Product] from list
     */
    fun removeProduct(product: Product) {
        val position = list.indexOf(product)
        if(position > -1) {
            list.remove(product)
            notifyItemRemoved(position)
        }
    }

    companion object {
        private const val TAG = "ProductAdapter"
    }
}