package com.example.powerquest.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.powerquest.R
import com.example.powerquest.adapter.DetailAdapter
import com.example.powerquest.data.ExerciseItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DetailCustom : AppCompatActivity() {

    private lateinit var startCustomButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextView: TextView
    private var selectedExercises: ArrayList<ExerciseItem>? = null
    private var selectedDay: String? = null

    // Activity Result Launcher untuk DetailEdit
    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedExercises = result.data?.getParcelableArrayListExtra("selected_exercises")
            updateFirebaseData() // Perbarui Firebase dengan data baru
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_custom)

        recyclerView = findViewById(R.id.list_recycler_view)
        editTextView = findViewById(R.id.textEdit)
        startCustomButton = findViewById(R.id.start_custom)

        // Ambil data dari Intent
        selectedExercises = intent.getParcelableArrayListExtra("selected_exercises")
        selectedDay = intent.getStringExtra("selected_day")

        setupRecyclerView()

        // Listener untuk EditText
        editTextView.setOnClickListener {
            val intent = Intent(this, DetailEdit::class.java)
            intent.putParcelableArrayListExtra("selected_exercises", selectedExercises)
            intent.putExtra("selected_day", selectedDay) // Kirimkan selected_day
            editLauncher.launch(intent)
        }

        // Listener untuk Tombol Mulai
        startCustomButton.setOnClickListener {
            if (selectedExercises.isNullOrEmpty()) {
                Toast.makeText(this, "Tidak ada latihan untuk memulai!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, StartCustom::class.java).apply {
                putParcelableArrayListExtra("selected_exercises", selectedExercises)
                putExtra("selected_day", selectedDay)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DetailAdapter(selectedExercises ?: mutableListOf()) { updatedExercise ->
            val index = selectedExercises?.indexOfFirst { it.title == updatedExercise.title }
            if (index != null && index >= 0) {
                selectedExercises!![index] = updatedExercise
                recyclerView.adapter?.notifyItemChanged(index)
            }
        }
    }

    // Update Firebase dengan data terbaru
    private fun updateFirebaseData() {
        if (selectedDay != null && !selectedExercises.isNullOrEmpty()) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            FirebaseDatabase.getInstance()
                .reference
                .child("user_progress")
                .child(userId)
                .child("custom_schedule")
                .child(selectedDay!!)
                .child("exercises")
                .setValue(selectedExercises)
                .addOnSuccessListener {
                    setupRecyclerView() // Refresh RecyclerView
                    showToast("Data berhasil diperbarui!")
                }
                .addOnFailureListener { exception ->
                    showToast("Gagal memperbarui data: ${exception.message}")
                }
        }
    }

    // Notifikasi jika gagal memperbarui
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
