package br.unisanta.tp_room.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.unisanta.tp_room.dao.ProductDao
import br.unisanta.tp_room.databinding.ActivityMainBinding
import br.unisanta.tp_room.db.AppDatabase
import br.unisanta.tp_room.model.Product
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration()
            .build()

        productDao = db.productDao()

        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString().trim()
            val priceText = binding.editProductPrice.text.toString().trim()
            val description = binding.editProductDescription.text.toString().trim()
            val quantityText = binding.editProductQuantity.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Preencha o nome do produto!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0.0) {
                binding.editProductPrice.error = "Informe um preço válido"
                binding.editProductPrice.requestFocus()
                return@setOnClickListener
            }

            val quantity = quantityText.toIntOrNull() ?: 1

            val product = Product(
                name = name,
                price = price,
                description = description,
                quantity = quantity
            )

            lifecycleScope.launch {
                productDao.insertAll(product)
                Log.i("TAG", "Produto inserido: $name R$$price (Qtd: $quantity)")
                Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

                binding.editProductName.text.clear()
                binding.editProductPrice.text.clear()
                binding.editProductDescription.text.clear()
                binding.editProductQuantity.text.clear()
                binding.editProductName.requestFocus()

                atualizarListaNaTela(productDao.getAll())
            }
        }

        binding.btnListarProdutos.setOnClickListener {
            lifecycleScope.launch {
                val products = productDao.getAll()
                atualizarListaNaTela(products)
            }
        }

        binding.btnBuscarNome.setOnClickListener {
            val termo = binding.editBuscarNome.text.toString().trim()
            lifecycleScope.launch {
                val filtrados = productDao.buscarPorNome("%$termo%")
                atualizarListaNaTela(filtrados)
                Toast.makeText(this@MainActivity, "Encontrados: ${filtrados.size}", Toast.LENGTH_SHORT).show()
            }
        }

        // BOTÃO ATUALIZAR PREÇO (Item A7)
        binding.btnAtualizarPreco.setOnClickListener {
            val id = binding.editProductId.text.toString().toIntOrNull()
            val novoPreco = binding.editNovoPreco.text.toString().toDoubleOrNull()

            if (id == null || novoPreco == null || novoPreco <= 0.0) {
                Toast.makeText(this, "Informe um ID e um novo preço válido!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produtoAtual = productDao.getById(id)
                if (produtoAtual != null) {
                    val produtoAtualizado = produtoAtual.copy(price = novoPreco)
                    productDao.update(produtoAtualizado)
                    Toast.makeText(this@MainActivity, "Preço do ID $id atualizado!", Toast.LENGTH_SHORT).show()
                    binding.editProductId.text.clear()
                    binding.editNovoPreco.text.clear()
                    atualizarListaNaTela(productDao.getAll())
                } else {
                    Toast.makeText(this@MainActivity, "Produto com ID $id não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnExcluirPorId.setOnClickListener {
            val id = binding.editProductId.text.toString().toIntOrNull()
            if (id == null) {
                Toast.makeText(this, "Informe o ID para excluir!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produto = productDao.getById(id)
                if (produto != null) {
                    productDao.delete(produto)
                    Toast.makeText(this@MainActivity, "Produto ID $id excluído!", Toast.LENGTH_SHORT).show()
                    binding.editProductId.text.clear()
                    atualizarListaNaTela(productDao.getAll())
                } else {
                    Toast.makeText(this@MainActivity, "ID $id não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnExcluirTodos.setOnClickListener {
            lifecycleScope.launch {
                productDao.deleteAll()
                Toast.makeText(this@MainActivity, "Todos os produtos foram excluídos!", Toast.LENGTH_SHORT).show()
                atualizarListaNaTela(emptyList())
            }
        }
    }

    private fun atualizarListaNaTela(products: List<Product>) {
        val quantidadeCadastrados = products.size
        val somaPrecos = products.sumOf { it.price }

        binding.txtTotalEstoque.text =
            "Total de produtos: $quantidadeCadastrados | Soma: R$ ${"%.2f".format(somaPrecos)}"

        if (products.isNotEmpty()) {
            val textoFormatado = products.joinToString(separator = "\n\n") {
                Log.i("TAG", "Produto = ${it.name}, Preço = R$${it.price}, Descrição = ${it.description}, Qtd = ${it.quantity}")
                "[ID: ${it.id}] ${it.name} - R$ ${"%.2f".format(it.price)}\nQtd: ${it.quantity} | Desc: ${it.description ?: "-"}"
            }
            // Item A3: Lista no TextView
            binding.txtListaProdutos.text = textoFormatado
        } else {
            Log.i("TAG", "Nenhum produto encontrado.")
            binding.txtListaProdutos.text = "Nenhum produto cadastrado."
        }
    }
}