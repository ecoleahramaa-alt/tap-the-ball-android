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
    private lateinit var shell: LinearLayout
    private lateinit var contentScroll: ScrollView
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
        showSplash()
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
        Button(this).apply {
            text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
            minHeight = dp(48); setPadding(dp(14), dp(9), dp(14), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply { setColor(green); cornerRadius = dp(14).toFloat() }
            setOnClickListener { action() }
        }
    private fun styleInput(input: EditText): EditText = input.apply {
        setTextColor(ink); setHintTextColor(Color.rgb(112, 130, 121)); textSize = 16f
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.rgb(205, 224, 213))
        }
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(18).toFloat()
            setStroke(dp(1), Color.rgb(225, 237, 229))
        }
    }
    private fun showSplash() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5,65,48), Color.rgb(8,127,91), Color.rgb(31,174,112))
            )
        }
        val logo = ImageView(this).apply {
            setImageResource(com.scanfoodai.nutrition.R.drawable.ic_scanfood_logo)
            layoutParams = LinearLayout.LayoutParams(dp(126), dp(126)).apply { bottomMargin = dp(24) }
            contentDescription = "ScanFood AI logo"
        }
        page.addView(logo)
        page.addView(text("ScanFood AI", 34f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        page.addView(text("FOOD SCANNER • NUTRITION GUIDE", 12f, Color.rgb(222,255,231), true).apply { gravity = Gravity.CENTER; letterSpacing = 0.08f })
        page.addView(text("Comprenez ce que vous mangez.\nFaites des choix éclairés, produit après produit.", 17f, Color.WHITE).apply { gravity = Gravity.CENTER; setLineSpacing(dp(5).toFloat(), 1f) })
        val features = card()
        listOf("▦  Scanner les codes-barres", "◉  Lire nutrition et ingrédients", "♡  Garder favoris et historique").forEach { features.addView(text(it,15f,ink,true)) }
        page.addView(features)
        val start = button("Commencer  →") { buildHome() }.apply {
            textSize = 17f; setTextColor(green)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=dp(18).toFloat() }
        }
        page.addView(start, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin=dp(22) })
        setContentView(page)
    }

    private fun buildHome() {
        shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(pale) }
        contentScroll = ScrollView(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(24)); setBackgroundColor(pale) }
        contentScroll.addView(root)
        shell.addView(contentScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(4), dp(5), dp(4), dp(5)); setBackgroundColor(Color.WHITE)
            elevation = dp(10).toFloat()
        }
        listOf(
            Triple("⌂", "Accueil") { showHome() },
            Triple("▣", "Scanner") { scan() },
            Triple("◷", "Historique") { showHistory() },
            Triple("♡", "Favoris") { showFavorites() },
            Triple("•••", "Plus") { showMore() }
        ).forEach { item ->
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(3), dp(4), dp(3), dp(4)) }
            col.addView(text(item.first, 21f, green, true).apply { gravity = Gravity.CENTER })
            col.addView(text(item.second, 10f, muted, true).apply { gravity = Gravity.CENTER })
            col.setOnClickListener { item.third.invoke() }
            nav.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        shell.addView(nav, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(shell)
        showHome()
    }
    private fun resetContent() {
        root.removeAllViews()
        contentScroll.scrollTo(0, 0)
    }
    private fun showHome() {
        resetContent()
        root.addView(text("🥑 ScanFood AI     ⚙", 27f, green, true))
        root.addView(text("Scannez • Découvrez • Mangez mieux", 14f, muted))
        val banner = card().apply { setBackgroundColor(Color.rgb(8, 127, 91)); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(8,127,91)); cornerRadius=dp(22).toFloat() } }
        banner.addView(text("Mangez plus malin 🌿", 23f, Color.WHITE, true))
        banner.addView(text("Découvrez ce que contient votre alimentation.", 14f, Color.WHITE))
        banner.addView(button("📷  Scanner un code-barres", ::scan).apply { backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(20, 185, 105)) }, margin())
        root.addView(banner, margin())
        root.addView(text("Recherche rapide", 19f, ink, true), margin())
        searchInput = styleInput(EditText(this).apply { hint = "Nom du produit, marque ou code-barres"; setSingleLine(true); inputType = InputType.TYPE_CLASS_TEXT })
        root.addView(searchInput, margin())
        root.addView(button("🔎  Rechercher un produit", ::search), margin())
        val shortcuts = listOf(
            Triple("⚖️", "Comparer", { showCompare() }),
            Triple("🎯", "Mes objectifs", { showGoals() }),
            Triple("🛒", "Liste de courses", { showShopping() }),
            Triple("⚙️", "Paramètres", { showSettings() })
        )
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shortcuts.forEach { item ->
            val tile = card().apply { gravity = Gravity.CENTER; setPadding(dp(6),dp(12),dp(6),dp(12)) }
            tile.addView(text(item.first, 24f, green, true).apply { gravity = Gravity.CENTER })
            tile.addView(text(item.second, 11f, ink, true).apply { gravity = Gravity.CENTER })
            tile.setOnClickListener { item.third.invoke() }
            row.addView(tile, LinearLayout.LayoutParams(0, dp(86), 1f).apply { marginEnd=dp(5) })
        }
        root.addView(row, margin())
        root.addView(text("Catégories populaires", 19f, ink, true), margin())
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("🥛 Laitiers", "🍎 Fruits", "🥣 Céréales", "🥤 Boissons").forEach { cat ->
            categories.addView(button(cat, { searchInput.setText(cat.substringAfter(" ").trim()); search() }), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd=dp(4) })
        }
        root.addView(categories, margin())
        status = text("Prêt à scanner. Internet requis pour les fiches en ligne.", 13f, muted)
        root.addView(status, margin())
        root.addView(text("Vos derniers produits", 19f, ink, true), margin())
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox)
        showSavedInto("history", resultBox)
        root.addView(text("Les informations et évaluations dépendent des données disponibles pour chaque produit. Elles ne remplacent pas un avis médical.", 12f, muted), margin())
    }
    private fun showMore() {
        resetContent()
        root.addView(text("Plus de ScanFood AI", 26f, green, true))
        root.addView(text("Tous vos outils au même endroit", 14f, muted))
        listOf(
            "⚖️ Comparer des produits" to { showCompare() },
            "🎯 Objectifs nutritionnels" to { showGoals() },
            "🛒 Liste de courses" to { showShopping() },
            "❤️ Favoris" to { showFavorites() },
            "🕘 Historique" to { showHistory() },
            "⚙️ Paramètres et préférences" to { showSettings() }
        ).forEach { item -> root.addView(button(item.first, item.second), margin()) }
    }
    private fun showCompare() {
        resetContent()
        root.addView(text("⚖️ Comparer les produits", 25f, green, true))
        root.addView(text("Entrez deux noms ou codes-barres. Les valeurs sont comparées pour 100 g/100 ml.", 14f, muted))
        root.addView(text("Premier produit", 16f, ink, true), margin())
        val a = styleInput(EditText(this).apply { hint="Nom ou code-barres du premier produit"; setSingleLine(true); setText(searchInput.text.toString()) })
        root.addView(a, margin())
        root.addView(text("Deuxième produit", 16f, ink, true), margin())
        val b = styleInput(EditText(this).apply { hint="Nom ou code-barres du deuxième produit"; setSingleLine(true) })
        root.addView(b, margin())
        val results = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(button("Comparer les valeurs nutritionnelles", {
            val q1=a.text.toString().trim(); val q2=b.text.toString().trim()
            if(q1.isBlank() || q2.isBlank()) { Toast.makeText(this,"Remplissez les deux champs",Toast.LENGTH_SHORT).show() }
            else {
                results.removeAllViews()
                results.addView(text("Recherche des deux fiches…",14f,muted))
                executor.execute {
                    try {
                        val p1=lookupProduct(q1); val p2=lookupProduct(q2)
                        runOnUiThread {
                            results.removeAllViews()
                            if(p1==null || p2==null) results.addView(text("Un des produits est introuvable. Essayez le code-barres exact ou un nom plus précis.",14f,muted))
                            else {
                                val n1=p1.optJSONObject("nutriments") ?: JSONObject()
                                val n2=p2.optJSONObject("nutriments") ?: JSONObject()
                                val table=card()
                                table.addView(text(p1.optString("product_name","Produit 1")+"  VS  "+p2.optString("product_name","Produit 2"),18f,green,true))
                                table.addView(text("Comparaison pour 100 g / 100 ml",12f,muted))
                                val metrics=listOf(Triple("energy-kcal_100g","Calories","kcal"),Triple("sugars_100g","Sucres","g"),Triple("salt_100g","Sel","g"),Triple("saturated-fat_100g","Graisses saturées","g"),Triple("fiber_100g","Fibres","g"),Triple("proteins_100g","Protéines","g"))
                                metrics.forEach { m ->
                                    val v1=n1.optDouble(m.first,Double.NaN); val v2=n2.optDouble(m.first,Double.NaN)
                                    val line=when { v1.isNaN() || v2.isNaN() -> "—"; else -> String.format(java.util.Locale.US,"%.1f",v1)+" "+m.third+"     |     "+String.format(java.util.Locale.US,"%.1f",v2)+" "+m.third }
                                    table.addView(text(m.second+"\n"+line,14f,ink))
                                }
                                table.addView(text("Les valeurs manquantes ne sont pas estimées. Une valeur plus basse n'est pas toujours meilleure (par exemple les protéines ou les fibres).",12f,muted))
                                results.addView(table,margin())
                            }
                        }
                    } catch(e:Exception) { runOnUiThread { results.removeAllViews(); results.addView(text("Erreur de connexion. Vérifiez Internet puis réessayez.",14f,muted)) } }
                }
            }
        }), margin())
        root.addView(results, margin())
    }
    private fun lookupProduct(query: String): JSONObject? {
        val isBarcode=query.matches(Regex("\\d{8,14}"))
        val endpoint=if(isBarcode) "https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(query,"UTF-8")}.json" else "https://world.openfoodfacts.org/cgi/search.pl?search_terms=${URLEncoder.encode(query,"UTF-8")}&search_simple=1&action=process&json=1&page_size=1"
        val conn=(URL(endpoint).openConnection() as HttpURLConnection).apply { connectTimeout=12000; readTimeout=15000; setRequestProperty("User-Agent","ScanFoodAI/1.1 (Android nutrition app)") }
        val body=conn.inputStream.bufferedReader().use { it.readText() }; conn.disconnect()
        val json=JSONObject(body)
        return if(isBarcode) { if(json.optInt("status")==1) json.optJSONObject("product") else null } else json.optJSONArray("products")?.optJSONObject(0)
    }
    private fun showGoals() {
        resetContent()
        root.addView(text("🎯 Mes objectifs", 26f, green, true))
        root.addView(text("Définissez des repères personnels. Les objectifs sont enregistrés sur ce téléphone.", 14f, muted))
        val fields = listOf("Calories quotidiennes (kcal)" to "goal_calories", "Protéines (g)" to "goal_protein", "Sucres (g)" to "goal_sugar", "Sel (g)" to "goal_salt")
        val inputs = mutableListOf<Pair<String,EditText>>()
        fields.forEach { (label,key) ->
            root.addView(text(label, 15f, ink, true), margin())
            val input=EditText(this).apply { hint="Ex. 2000"; inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(prefs.getString(key,"")) }
            inputs.add(key to input); root.addView(input, margin())
        }
        root.addView(button("💾 Enregistrer mes objectifs", {
            inputs.forEach { (key,input) -> prefs.edit().putString(key,input.text.toString()).apply() }
            Toast.makeText(this,"Objectifs enregistrés",Toast.LENGTH_SHORT).show()
        }), margin())
        root.addView(text("Ces repères sont informatifs et ne constituent pas une prescription médicale.", 12f, muted), margin())
    }
    private fun showShopping() {
        resetContent()
        root.addView(text("🛒 Liste de courses", 26f, green, true))
        root.addView(text("Préparez votre prochaine visite au magasin.", 14f, muted))
        val input=styleInput(EditText(this).apply { hint="Ajouter un aliment"; setSingleLine(true) })
        root.addView(input, margin())
        val listBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        fun renderList() {
            listBox.removeAllViews()
            val list=savedList("shopping")
            for(i in 0 until list.length()) {
                val item=list.optJSONObject(i) ?: continue
                val cb=CheckBox(this).apply { text=item.optString("name"); setTextColor(ink) }
                listBox.addView(cb)
            }
            if(list.length()==0) listBox.addView(text("Votre liste est vide pour le moment.",14f,muted))
        }
        root.addView(button("＋ Ajouter à la liste", {
            val name=input.text.toString().trim()
            if(name.isNotBlank()) { val list=savedList("shopping"); list.put(JSONObject().put("name",name)); prefs.edit().putString("shopping",list.toString()).apply(); input.text.clear(); renderList() }
        }), margin())
        root.addView(text("À acheter", 18f, ink, true), margin()); root.addView(listBox, margin())
        root.addView(button("Effacer la liste", { prefs.edit().putString("shopping","[]").apply(); renderList() }), margin())
        renderList()
    }
    private fun showSettings() {
        resetContent()
        root.addView(text("⚙️ Paramètres", 26f, green, true))
        root.addView(text("Personnalisez votre expérience ScanFood AI.", 14f, muted))
        root.addView(text("Langue", 16f, ink, true), margin())
        val language=Spinner(this)
        language.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Français","العربية","English","Español"))
        val savedLang=prefs.getInt("language",0); language.setSelection(savedLang.coerceIn(0,3))
        root.addView(language, margin())
        root.addView(text("Préférences alimentaires", 16f, ink, true), margin())
        val prefsList=listOf("Éviter le sucre ajouté","Réduire le sel","Végétarien","Sans gluten","Sans lactose")
        prefsList.forEach { label -> val cb=CheckBox(this).apply { text=label; setTextColor(ink); isChecked=prefs.getBoolean("pref_"+label,false); setOnCheckedChangeListener { _,checked -> prefs.edit().putBoolean("pref_"+label,checked).apply() } }; root.addView(cb) }
        root.addView(button("💾 Enregistrer les paramètres", { prefs.edit().putInt("language",language.selectedItemPosition).apply(); Toast.makeText(this,"Paramètres enregistrés",Toast.LENGTH_SHORT).show() }), margin())
        root.addView(text("À propos", 18f, ink, true), margin())
        root.addView(text("ScanFood AI utilise les informations disponibles dans Open Food Facts. Les fiches peuvent être incomplètes ou inexactes.", 13f, muted))
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
    private fun showFavorites() = showSaved("favorites", "❤️ Mes favoris")
    private fun showHistory() = showSaved("history", "🕘 Historique")
    private fun showSaved(key: String, title: String) {
        resetContent()
        root.addView(text(title, 25f, green, true))
        root.addView(text("Vos produits enregistrés sur cet appareil.", 14f, muted))
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox, margin())
        showSavedInto(key, resultBox)
    }
    private fun showSavedInto(key: String, target: LinearLayout) {
        target.removeAllViews()
        val list = savedList(key)
        if (list.length() == 0) {
            val c = card(); c.addView(text(if(key=="history") "Les produits que vous scannez apparaîtront ici." else "Ajoutez des produits à vos favoris depuis leur fiche.", 14f, muted))
            target.addView(c, margin()); return
        }
        for (i in 0 until list.length()) {
            val item = list.optJSONObject(i) ?: continue
            val c = card()
            c.addView(text(item.optString("product_name", "Produit"), 17f, ink, true))
            c.addView(text(item.optString("brands", "Marque non renseignée"), 13f, muted))
            c.addView(text("Nutri-Score : " + item.optString("nutriscore_grade", "non disponible").uppercase(), 14f, green, true))
            c.addView(button("Voir la fiche", { resetContent(); resultBox=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; root.addView(text("Détail du produit",24f,green,true)); root.addView(resultBox); renderProduct(item,item.optString("code")) }), margin())
            target.addView(c, margin())
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
        val imageUrl = p.optString("image_front_url").ifBlank { p.optString("image_url") }
        if (imageUrl.startsWith("https://")) {
            val productImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180))
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.WHITE)
                contentDescription = "Image du produit"
            }
            c.addView(productImage, margin())
            executor.execute {
                try {
                    val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply { connectTimeout=8000; readTimeout=10000 }
                    val bitmap = connection.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                    connection.disconnect()
                    if (bitmap != null) runOnUiThread { productImage.setImageBitmap(bitmap) }
                } catch (_: Exception) { }
            }
        }
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
