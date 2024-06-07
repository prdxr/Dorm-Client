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
import com.example.dormproject.adapters.repair.ItemRepairAdapter
import com.example.dormproject.adapters.repair.ItemRepairAdapterDataClass
import com.example.dormproject.R
import com.example.dormproject.ApiService
import com.example.dormproject.retrofit.req.repair.RepairApi
import com.example.dormproject.retrofit.req.repair.data.ReqRepairGetAllRepairsListItem
import com.example.dormproject.retrofit.req.role.RoleApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeneralRepairsActivity : AppCompatActivity() {

    // Ленивая инициализация API для ролей и ремонтов
    private val roleApi: RoleApi by lazy { ApiService.createService(RoleApi::class.java) }
    private val repairApi: RepairApi by lazy { ApiService.createService(RepairApi::class.java) }
    private val itemList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.repairList) }
    private val navBar by lazy { findViewById<BottomNavigationView>(R.id.nav_bar) }
    private val addRepairButton by lazy { findViewById<Button>(R.id.general_repair_add) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включение поддержки отступов для системных окон
        setContentView(R.layout.activity_general_repairs)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Установка слушателей для элементов навигационной панели
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
                    Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                else -> false
            }
        }

        setNewList() // Установка нового списка ремонтов
        hideAddButton() // Скрытие кнопки добавления в зависимости от роли пользователя

        // Обработка нажатия кнопки добавления ремонта
        addRepairButton.setOnClickListener {
            startActivity(Intent(this, AddRepairActivity::class.java))
        }
    }

    // Метод для скрытия кнопки добавления в зависимости от роли пользователя
    private fun hideAddButton() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = roleApi.getMyRole()
                withContext(Dispatchers.Main) {
                    if (response.roleId == 3) { // Роль студента
                        addRepairButton.visibility = Button.GONE
                    } else {
                        addRepairButton.visibility = Button.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralRepairsActivity, "Проблема с удалением, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод для удаления элемента
    private fun deleteItem(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repairApi.reqRepairDeleteRepairByIdRequest(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GeneralRepairsActivity, "Заявка удалена", Toast.LENGTH_SHORT).show()
                    setNewList() // Обновление списка после удаления элемента
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralRepairsActivity, "Проблема с удалением, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод для установки нового списка ремонтов
    private fun setNewList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repairApi.reqRepairGetAllRepairs()
                withContext(Dispatchers.Main) {
                    val data = ItemRepairAdapterDataClass(response.guestRequestList as ArrayList<ReqRepairGetAllRepairsListItem>,
                        onClickDelete = { id ->
                            deleteItem(id)
                        }
                    ) { item ->
                        val newIntent = Intent(this@GeneralRepairsActivity, EditRepairActivity::class.java)
                        newIntent.putExtra("itemID", item.reqId)
                        startActivity(newIntent)
                    }
                    itemList.layoutManager = LinearLayoutManager(applicationContext)
                    itemList.adapter = ItemRepairAdapter(data)
                    if (response.guestRequestList.isEmpty()) {
                        Toast.makeText(this@GeneralRepairsActivity, "Заявок нет", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val newE = e.toString().replace("java.io.IOException: ", "")
                    Toast.makeText(this@GeneralRepairsActivity, "Проблема с подключением, Код ошибки: $newE", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
