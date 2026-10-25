package com.scanfood.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var resultBox: LinearLayout
    private lateinit var status: TextView
    private lateinit var searchInput: EditText
    private val green = Color.rgb(8, 127, 91)
    private val ink = Color.rgb(23, 42, 36)
    private val muted = Color.rgb(99, 116, 108)
    private val pale = Color.rgb(239, 248, 243)
    private val prefs by lazy { getSharedPreferences("scanfood_local", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(6, 78, 59)
        window.navigationBarColor = Color.rgb(7, 27, 22)
        buildHome()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(3), dp(2), dp(3))
        }
    }
    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply { text = label; isAllCaps = false; setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(green); setOnClickListener { action() } }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.rgb(225, 237, 229))
        }
    }
    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(24))
            setBackgroundColor(pale)
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        root.addView(text("🥑 ScanFood AI", 29f, green, true))
        root.addView(text("Scan. Comprends. Choisis mieux.", 15f, muted))
        val banner = card()
        banner.setBackgroundColor(Color.rgb(224, 245, 233))
        banner.addView(text("Mange plus malin 🌿", 22f, green, true))
        banner.addView(text("Scanne le code-barres d’un aliment pour consulter ses informations nutritionnelles, ingrédients et allergènes.", 15f))
        root.addView(banner, margin())
        root.addView(text("Rechercher un produit", 19f, ink, true))
        searchInput = EditText(this).apply {
            hint = "Nom du produit ou code-barres (EAN)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.LTGRAY)
            }
        }
        root.addView(searchInput, margin())
        root.addView(button("🔎 Rechercher", ::search), margin())
        root.addView(button("📷 Scanner un code-barres", ::scan), margin())
        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        quick.addView(button("❤️ Favoris", ::showFavorites), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        quick.addView(button("🕘 Historique", ::showHistory), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        root.addView(quick, margin())
        status = text("Prêt à rechercher un aliment. Internet nécessaire pour récupérer les données.", 14f, muted)
        root.addView(status, margin())
        root.addView(text("Que vérifie l’application ?", 20f, ink, true), margin())
        val features = card()
        listOf(
            "🥦 Nutri-Score et nutrition (si disponibles)",
            "🍬 Sucres, graisses, sel, calories et protéines",
            "🧾 Liste des ingrédients et additifs renseignés",
            "⚠️ Allergènes déclarés sur la fiche produit",
            "🌍 Données de produits de nombreux pays"
        ).forEach { features.addView(text(it, 15f)) }
        root.addView(features, margin())
        root.addView(text("Résultat", 20f, ink, true), margin())
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox)
        showMessage("Les résultats dépendent des informations présentes dans la base de données.")
        root.addView(text("Important : le score est indicatif. Il ne remplace pas un avis médical et un produit ne peut pas être évalué correctement si ses données sont incomplètes.", 12f, muted), margin())
    }
    private fun margin(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
    private fun scan() {
        try {
            com.google.zxing.integration.android.IntentIntegrator(this)
                .setPrompt("Place le code-barres dans le cadre")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.ALL_CODE_TYPES)
                .initiateScan()
        } catch (e: Exception) {
            status.text = "Impossible d’ouvrir le scanner : ${e.message ?: "erreur"}"
        }
    }

    @Deprecated("Legacy scanner result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val code = result.contents
            if (code.isNullOrBlank()) status.text = "Scan annulé." else {
                searchInput.setText(code)
                search()
            }
        }
    }
    private fun search() {
        val q = searchInput.text.toString().trim()
        if (q.isEmpty()) { searchInput.error = "Saisis un nom ou scanne un code"; return }
        status.text = "Recherche en cours…"
        resultBox.removeAllViews()
        executor.execute {
            try {
                val isBarcode = q.matches(Regex("\\d{8,14}"))
                val endpoint = if (isBarcode)
                    "https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(q, "UTF-8")}.json"
                else
                    "https://world.openfoodfacts.org/cgi/search.pl?search_terms=${URLEncoder.encode(q, "UTF-8")}&search_simple=1&action=process&json=1&page_size=1"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000; readTimeout = 15000
                    setRequestProperty("User-Agent", "ScanFoodAI/1.0 (Android food information app)")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val product = if (isBarcode) {
                    if (json.optInt("status") == 1) json.optJSONObject("product") else null
                } else {
                    json.optJSONArray("products")?.optJSONObject(0)
                }
                runOnUiThread {
                    if (product == null) {
                        status.text = "Produit introuvable. Vérifie le code ou essaie le nom."
                        showMessage("Aucune fiche trouvée. Tous les produits ne sont pas enregistrés.")
                    } else {
                        status.text = "Fiche produit chargée."
                        renderProduct(product, q)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Connexion impossible. Vérifie Internet puis réessaie."
                    showMessage("Les informations du produit n’ont pas pu être chargées.")
                }
            }
        }
    }
    private fun savedList(key: String): org.json.JSONArray {
        return try { org.json.JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { org.json.JSONArray() }
    }
    private fun addSaved(key: String, product: JSONObject) {
        val list = savedList(key)
        val code = product.optString("code")
        val name = product.optString("product_name", "Produit")
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            if ((code.isNotBlank() && item.optString("code") == code) || (code.isBlank() && item.optString("product_name") == name)) { list.remove(i); break }
        }
        val item = JSONObject()
        item.put("code", code); item.put("product_name", name); item.put("brands", product.optString("brands"))
        item.put("nutriscore_grade", product.optString("nutriscore_grade", product.optString("nutrition_grades")))
        item.put("ingredients_text", product.optString("ingredients_text")); item.put("allergens", product.optString("allergens"))
        item.put("nutriments", product.optJSONObject("nutriments") ?: JSONObject())
        list.put(0, item); while (list.length() > 100) list.remove(list.length() - 1)
        prefs.edit().putString(key, list.toString()).apply()
    }
    private fun showFavorites() = showSaved("favorites", "❤️ المنتوجات المفضلة")
    private fun showHistory() = showSaved("history", "🕘 آخر المنتوجات")
    private fun showSaved(key: String, title: String) {
        resultBox.removeAllViews(); resultBox.addView(text(title, 20f, green, true))
        val list = savedList(key)
        if (list.length() == 0) { showMessage("مازال ما كاين حتى منتوج محفوظ هنا."); return }
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val c = card(); c.addView(text(item.optString("product_name", "Produit"), 18f, ink, true))
            c.addView(text(item.optString("brands"), 13f, muted))
            c.addView(text("Nutri-Score : " + item.optString("nutri-score_grade", item.optString("nutriscore_grade", "غير متوفر")), 14f, green, true))
            c.addView(button("عرض التفاصيل", { renderProduct(item, item.optString("code")) }), margin())
            resultBox.addView(c, margin())
        }
    }
    private fun showMessage(message: String) {
        resultBox.removeAllViews()
        val c = card(); c.addView(text(message, 14f, muted)); resultBox.addView(c, margin())
    }
    private fun renderProduct(p: JSONObject, fallback: String) {
        addSaved("history", p)
        resultBox.removeAllViews()
        val name = p.optString("product_name").ifBlank { "Produit sans nom renseigné" }
        val brand = p.optString("brands").ifBlank { "Marque non renseignée" }
        val nutri = p.optString("nutriscore_grade").ifBlank { p.optString("nutrition_grades") }.uppercase()
        val ingredients = p.optString("ingredients_text").ifBlank { "Ingrédients non renseignés." }
        val allergens = p.optString("allergens").ifBlank { "Aucun renseignement disponible sur les allergènes." }
        val nutriments = p.optJSONObject("nutriments")
        val c = card()
        c.addView(text(name, 22f, ink, true))
        c.addView(text("Marque : $brand", 15f, muted))
        if (nutri.isNotBlank()) {
            val scoreColor = when(nutri.firstOrNull()?.lowercaseChar()) { 'a','b' -> Color.rgb(27,140,75); 'c' -> Color.rgb(219,163,18); 'd','e' -> Color.rgb(203,70,55); else -> muted }
            c.addView(text("Nutri-Score : $nutri", 20f, scoreColor, true))
            c.addView(text("Le Nutri-Score est un indicateur nutritionnel, pas une évaluation médicale complète.", 12f, muted))
        } else c.addView(text("Nutri-Score non disponible", 15f, muted))
        c.addView(text("Valeurs pour 100 g / 100 ml", 17f, green, true))
        fun nutrient(key: String, label: String, unit: String) {
            val v = nutriments?.opt(key)
            val value = when(v) { null, JSONObject.NULL -> "—"; is Number -> String.format(java.util.Locale.US, "%.1f", v.toDouble()) + " $unit"; else -> v.toString() }
            c.addView(text("$label : $value", 15f))
        }
        nutrient("energy-kcal_100g", "Énergie", "kcal")
        nutrient("sugars_100g", "Sucres", "g")
        nutrient("fat_100g", "Matières grasses", "g")
        nutrient("saturated-fat_100g", "Graisses saturées", "g")
        nutrient("salt_100g", "Sel", "g")
        nutrient("proteins_100g", "Protéines", "g")
        c.addView(text("Ingrédients", 17f, green, true))
        c.addView(text(ingredients, 14f))
        c.addView(text("Allergènes déclarés", 17f, green, true))
        c.addView(text(allergens, 14f))
        val categories = p.optString("categories").ifBlank { "Catégorie non renseignée" }
        c.addView(text("Catégories : $categories", 13f, muted))
        c.addView(text("Code : ${p.optString("code", fallback)}", 12f, muted))
        c.addView(button("❤️ Ajouter / retirer des favoris", { addSaved("favorites", p); Toast.makeText(this, "Favoris mis à jour", Toast.LENGTH_SHORT).show() }), margin())
        resultBox.addView(c, margin())
    }
    override fun onDestroy() { super.onDestroy(); executor.shutdownNow() }
}
