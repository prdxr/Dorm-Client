package com.example.dormproject.activitys

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dormproject.adapters.guest.ItemGuestAdapter
import com.example.dormproject.adapters.guest.ItemGuestAdapterDataClass
import com.example.dormproject.R
import com.example.dormproject.ApiService
import com.example.dormproject.CookieManager
import com.example.dormproject.retrofit.req.guest.GuestApi
import com.example.dormproject.retrofit.req.guest.data.ReqGuestGetAllGuestsListItem
import com.example.dormproject.retrofit.req.role.RoleApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeneralGuestsActivity : AppCompatActivity() {

    // Ленивая инициализация API для ролей и гостей
    private val roleApi: RoleApi by lazy { ApiService.createService(RoleApi::class.java) }
    private val guestApi: GuestApi by lazy { ApiService.createService(GuestApi::class.java) }
    private val itemList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.guestList) }
    private val navBar by lazy { findViewById<BottomNavigationView>(R.id.nav_bar_guest) }
    private val addGuestButton by lazy { findViewById<Button>(R.id.general_guest_add) }

    override fun onCreate(savedInstanceState: Bundle?) {

        lateinit var cookieManager: CookieManager

        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включение поддержки отступов для системных окон
        setContentView(R.layout.activity_general_guests)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navBar.selectedItemId = R.id.menu_guests

        // Настройка действий для элементов навигационной панели
        navBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_guests -> {
                    startActivity(Intent(this, GeneralGuestsActivity::class.java))
                    true
                }
                R.id.menu_repairs -> {
                    startActivity(Intent(this, GeneralRepairsActivity::class.java))
                    true
                }
                R.id.menu_logout -> {
                    ApiService.logout() // Выход из аккаунта
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }

        setNewList() // Установка нового списка гостей
        hideAddButton() // Скрытие кнопки добавления

        // Обработка нажатия кнопки добавления гостя
        addGuestButton.setOnClickListener {
            startActivity(Intent(this, AddGuestActivity::class.java))
        }
    }

    // Метод для скрытия кнопки добавления в зависимости от роли пользователя
    private fun hideAddButton() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = roleApi.getMyRole()
                withContext(Dispatchers.Main) {
                    if (response.roleId == 3) {
                        addGuestButton.visibility = Button.GONE
                    } else {
                        addGuestButton.visibility = Button.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralGuestsActivity, "Проблема с получением данных, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод для удаления элемента
    private fun deleteItem(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                guestApi.reqGuestDeleteGuestByIdRequest(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GeneralGuestsActivity, "Заявка удалена", Toast.LENGTH_SHORT).show()
                    setNewList() // Обновление списка после удаления элемента
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralGuestsActivity, "Проблема с удалением, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод для установки нового списка гостей
    private fun setNewList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = guestApi.reqGuestGetAllGuests()
                withContext(Dispatchers.Main) {
                    val data = ItemGuestAdapterDataClass(response.guestRequestList as ArrayList<ReqGuestGetAllGuestsListItem>,
                        onClickDelete = { id ->
                            deleteItem(id)
                        }
                    ) { item ->
                        val newIntent = Intent(this@GeneralGuestsActivity, EditGuestActivity::class.java)
                        newIntent.putExtra("itemID", item.reqId)
                        startActivity(newIntent)
                    }
                    itemList.layoutManager = LinearLayoutManager(applicationContext)
                    itemList.adapter = ItemGuestAdapter(data)
                    if (response.guestRequestList.isEmpty()) {
                        Toast.makeText(this@GeneralGuestsActivity, "Заявок нет", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralGuestsActivity, "Проблема с подключением, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
