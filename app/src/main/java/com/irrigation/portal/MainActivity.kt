package com.irrigation.portal

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

data class DocRecord(
    val id: String,
    val title: String,
    val refNo: String,
    val category: String,
    val date: String,
    val keywords: String,
    val driveUrl: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var homeMenuContainer: ScrollView
    private lateinit var listContainer: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var categoryCheckboxGroup: LinearLayout
    private lateinit var cbFinancial: CheckBox
    private lateinit var cbTechnical: CheckBox
    private lateinit var cbAdministrative: CheckBox
    private lateinit var cbRevenue: CheckBox
    private lateinit var spinnerSort: Spinner
    private lateinit var tvResultCount: TextView
    private lateinit var recyclerView: RecyclerView

    private val fullDocsList = mutableListOf<DocRecord>()
    private val notificationsList = mutableListOf<DocRecord>()
    private val displayedList = mutableListOf<DocRecord>()
    private lateinit var adapter: DocAdapter
    private var currentScreen = "HOME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadJsonAssets()
        bindViews()
        setupListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentScreen != "HOME") showHome() else finish()
            }
        })
    }

    private fun loadJsonAssets() {
        val fullJson = assets.open("full_documents.json").bufferedReader().use { it.readText() }
        val fullArr = JSONArray(fullJson)
        for (i in 0 until fullArr.length()) {
            val obj = fullArr.getJSONObject(i)
            fullDocsList.add(
                DocRecord(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    refNo = "",
                    category = "Full Document",
                    date = "",
                    keywords = obj.optString("title").lowercase(),
                    driveUrl = obj.optString("drive_url")
                )
            )
        }

        val notifJson = assets.open("notifications.json").bufferedReader().use { it.readText() }
        val notifArr = JSONArray(notifJson)
        for (i in 0 until notifArr.length()) {
            val obj = notifArr.getJSONObject(i)
            notificationsList.add(
                DocRecord(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    refNo = obj.optString("ref_no"),
                    category = obj.optString("category"),
                    date = obj.optString("date"),
                    keywords = obj.optString("keywords").lowercase(),
                    driveUrl = obj.optString("drive_url")
                )
            )
        }
    }

    private fun bindViews() {
        homeMenuContainer = findViewById(R.id.homeMenuContainer)
        listContainer = findViewById(R.id.listContainer)
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        etSearch = findViewById(R.id.etSearch)
        categoryCheckboxGroup = findViewById(R.id.categoryCheckboxGroup)
        cbFinancial = findViewById(R.id.cbFinancial)
        cbTechnical = findViewById(R.id.cbTechnical)
        cbAdministrative = findViewById(R.id.cbAdministrative)
        cbRevenue = findViewById(R.id.cbRevenue)
        spinnerSort = findViewById(R.id.spinnerSort)
        tvResultCount = findViewById(R.id.tvResultCount)
        recyclerView = findViewById(R.id.recyclerView)

        val sortOptions = arrayOf(
            "Alphabetical (A to Z)",
            "Alphabetical (Z to A)",
            "Date (Newest First)",
            "Date (Oldest First)"
        )
        spinnerSort.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortOptions)

        adapter = DocAdapter(this, displayedList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { showHome() }

        findViewById<View>(R.id.cardFullDocument).setOnClickListener { openSection("FULL_DOCS") }
        findViewById<View>(R.id.cardNotifications).setOnClickListener { openSection("NOTIFICATIONS") }
        findViewById<View>(R.id.cardOtherData).setOnClickListener { openSection("OTHER_DATA") }
        findViewById<View>(R.id.cardBookmarks).setOnClickListener { openSection("BOOKMARKS") }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val cbListener = CompoundButton.OnCheckedChangeListener { _, _ -> applyFilters() }
        cbFinancial.setOnCheckedChangeListener(cbListener)
        cbTechnical.setOnCheckedChangeListener(cbListener)
        cbAdministrative.setOnCheckedChangeListener(cbListener)
        cbRevenue.setOnCheckedChangeListener(cbListener)

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showHome() {
        currentScreen = "HOME"
        tvHeaderTitle.text = "Reference & Policy Portal"
        btnBack.visibility = View.GONE
        homeMenuContainer.visibility = View.VISIBLE
        listContainer.visibility = View.GONE
    }

    private fun openSection(section: String) {
        currentScreen = section
        btnBack.visibility = View.VISIBLE
        homeMenuContainer.visibility = View.GONE
        listContainer.visibility = View.VISIBLE
        etSearch.setText("")

        when (section) {
            "FULL_DOCS" -> {
                tvHeaderTitle.text = "Full Documents"
                categoryCheckboxGroup.visibility = View.GONE
            }
            "NOTIFICATIONS" -> {
                tvHeaderTitle.text = "Notifications & Policies"
                categoryCheckboxGroup.visibility = View.VISIBLE
                cbFinancial.isChecked = false
                cbTechnical.isChecked = false
                cbAdministrative.isChecked = false
                cbRevenue.isChecked = false
                spinnerSort.setSelection(0)
            }
            "BOOKMARKS" -> {
                tvHeaderTitle.text = "Bookmarks"
                categoryCheckboxGroup.visibility = View.GONE
            }
            "OTHER_DATA" -> {
                tvHeaderTitle.text = "Other Data"
                categoryCheckboxGroup.visibility = View.GONE
            }
        }
        applyFilters()
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()
        val prefs = getSharedPreferences("bookmarks_pref", Context.MODE_PRIVATE)
        val bookmarkedIds = prefs.getStringSet("saved_ids", emptySet()) ?: emptySet()

        val sourceList = when (currentScreen) {
            "FULL_DOCS" -> fullDocsList
            "NOTIFICATIONS" -> notificationsList
            "BOOKMARKS" -> (fullDocsList + notificationsList).filter { bookmarkedIds.contains(it.id) }
            else -> emptyList()
        }

        val selectedCategories = mutableSetOf<String>()
        if (cbFinancial.isChecked) selectedCategories.add("Financial")
        if (cbTechnical.isChecked) selectedCategories.add("Technical")
        if (cbAdministrative.isChecked) selectedCategories.add("Administrative")
        if (cbRevenue.isChecked) selectedCategories.add("Revenue")

        var filtered = sourceList.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query) ||
                item.keywords.contains(query) ||
                item.refNo.lowercase().contains(query)

            val matchesCategory = (currentScreen != "NOTIFICATIONS") ||
                selectedCategories.isEmpty() ||
                selectedCategories.contains(item.category)

            matchesQuery && matchesCategory
        }

        filtered = if (currentScreen == "NOTIFICATIONS") {
            when (spinnerSort.selectedItemPosition) {
                0 -> filtered.sortedBy { it.title.lowercase() }
                1 -> filtered.sortedByDescending { it.title.lowercase() }
                2 -> filtered.sortedByDescending { it.date }
                3 -> filtered.sortedBy { it.date }
                else -> filtered
            }
        } else {
            filtered.sortedBy { it.title.lowercase() }
        }

        displayedList.clear()
        displayedList.addAll(filtered)
        tvResultCount.text = "Showing ${displayedList.size} items"
        adapter.notifyDataSetChanged()
    }

    class DocAdapter(
        private val context: Context,
        private val items: List<DocRecord>
    ) : RecyclerView.Adapter<DocAdapter.ViewHolder>() {

        private val prefs = context.getSharedPreferences("bookmarks_pref", Context.MODE_PRIVATE)

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvDocTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvDocMeta)
            val btnBookmark: ImageButton = view.findViewById(R.id.btnBookmark)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
            return ViewHolder(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            holder.tvMeta.text = if (item.date.isNotEmpty()) {
                "${item.category}  •  Ref: ${item.refNo}  •  Date: ${item.date}"
            } else {
                "Full Reference Document (Google Drive PDF)"
            }

            // Open Google Drive Hyperlink on click
            holder.tvTitle.setOnClickListener {
                if (item.driveUrl.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.driveUrl))
                    context.startActivity(intent)
                }
            }

            val saved = prefs.getStringSet("saved_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val isBookmarked = saved.contains(item.id)
            holder.btnBookmark.setImageResource(
                if (isBookmarked) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            holder.btnBookmark.setOnClickListener {
                val currentSaved = prefs.getStringSet("saved_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                if (currentSaved.contains(item.id)) currentSaved.remove(item.id) else currentSaved.add(item.id)
                prefs.edit().putStringSet("saved_ids", currentSaved).apply()
                notifyItemChanged(position)
            }
        }
    }
}
