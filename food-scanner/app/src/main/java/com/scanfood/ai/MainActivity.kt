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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        showSplash()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun currentLanguage(): String = prefs.getString("language_code", "fr") ?: "fr"
    private val translations = mapOf(
        "Accueil" to mapOf("ar" to "الرئيسية", "en" to "Home", "es" to "Inicio", "de" to "Start", "it" to "Home", "pt" to "Início", "nl" to "Home"),
        "Scanner" to mapOf("ar" to "مسح", "en" to "Scanner", "es" to "Escanear", "de" to "Scannen", "it" to "Scansiona", "pt" to "Ler código", "nl" to "Scannen"),
        "Historique" to mapOf("ar" to "السجل", "en" to "History", "es" to "Historial", "de" to "Verlauf", "it" to "Cronologia", "pt" to "Histórico", "nl" to "Geschiedenis"),
        "Favoris" to mapOf("ar" to "المفضلة", "en" to "Favorites", "es" to "Favoritos", "de" to "Favoriten", "it" to "Preferiti", "pt" to "Favoritos", "nl" to "Favorieten"),
        "Plus" to mapOf("ar" to "المزيد", "en" to "More", "es" to "Más", "de" to "Mehr", "it" to "Altro", "pt" to "Mais", "nl" to "Meer"),
        "Commencer  →" to mapOf("ar" to "ابدأ ←", "en" to "Get started →", "es" to "Comenzar →", "de" to "Los geht's →", "it" to "Inizia →", "pt" to "Começar →", "nl" to "Start →"),
        "Recherche rapide" to mapOf("ar" to "بحث سريع", "en" to "Quick search", "es" to "Búsqueda rápida", "de" to "Schnellsuche", "it" to "Ricerca rapida", "pt" to "Pesquisa rápida", "nl" to "Snel zoeken"),
        "Comparer" to mapOf("ar" to "مقارنة", "en" to "Compare", "es" to "Comparar", "de" to "Vergleichen", "it" to "Confronta", "pt" to "Comparar", "nl" to "Vergelijken"),
        "Mangez plus malin 🌿" to mapOf("ar" to "اختر طعامك بذكاء 🌿", "en" to "Make smarter food choices 🌿", "es" to "Elige mejor tus alimentos 🌿", "de" to "Bewusster essen 🌿", "it" to "Scegli meglio cosa mangiare 🌿", "pt" to "Faça escolhas melhores 🌿", "nl" to "Maak slimmere voedselkeuzes 🌿"),
        "Découvrez ce que contient votre alimentation." to mapOf("ar" to "اكتشف مكونات طعامك.", "en" to "Discover what is in your food.", "es" to "Descubre qué contiene tu comida.", "de" to "Entdecke, was in deinem Essen steckt.", "it" to "Scopri cosa contiene il tuo cibo.", "pt" to "Descubra o que contém a sua comida.", "nl" to "Ontdek wat er in je eten zit."),
        "Nom du produit, marque ou code-barres" to mapOf("ar" to "اسم المنتج أو العلامة أو الرمز الشريطي", "en" to "Product name, brand or barcode", "es" to "Nombre, marca o código de barras", "de" to "Produktname, Marke oder Barcode", "it" to "Nome, marca o codice a barre", "pt" to "Nome, marca ou código de barras", "nl" to "Productnaam, merk of barcode"),
        "🔎  Rechercher un produit" to mapOf("ar" to "🔎 البحث عن منتج", "en" to "🔎 Search for a product", "es" to "🔎 Buscar producto", "de" to "🔎 Produkt suchen", "it" to "🔎 Cerca un prodotto", "pt" to "🔎 Pesquisar produto", "nl" to "🔎 Product zoeken"),
        "Vos derniers produits" to mapOf("ar" to "آخر المنتجات", "en" to "Your recent products", "es" to "Tus productos recientes", "de" to "Zuletzt angesehene Produkte", "it" to "Prodotti recenti", "pt" to "Produtos recentes", "nl" to "Recent bekeken producten"),
        "Plus de ScanFood AI" to mapOf("ar" to "المزيد من ScanFood AI", "en" to "More from ScanFood AI", "es" to "Más de ScanFood AI", "de" to "Mehr von ScanFood AI", "it" to "Altro da ScanFood AI", "pt" to "Mais do ScanFood AI", "nl" to "Meer van ScanFood AI"),
        "Tous vos outils au même endroit" to mapOf("ar" to "كل الأدوات في مكان واحد", "en" to "All your tools in one place", "es" to "Todas tus herramientas en un solo lugar", "de" to "Alle Werkzeuge an einem Ort", "it" to "Tutti gli strumenti in un unico posto", "pt" to "Todas as ferramentas num só lugar", "nl" to "Al je tools op één plek"),
        "⚖️ Comparer des produits" to mapOf("ar" to "⚖️ مقارنة المنتجات", "en" to "⚖️ Compare products", "es" to "⚖️ Comparar productos", "de" to "⚖️ Produkte vergleichen", "it" to "⚖️ Confronta prodotti", "pt" to "⚖️ Comparar produtos", "nl" to "⚖️ Producten vergelijken"),
        "⚙️ À propos et source des données" to mapOf("ar" to "⚙️ حول التطبيق ومصدر البيانات", "en" to "⚙️ About & data source", "es" to "⚙️ Acerca de y fuente de datos", "de" to "⚙️ Info & Datenquelle", "it" to "⚙️ Info e fonte dei dati", "pt" to "⚙️ Sobre e fonte de dados", "nl" to "⚙️ Over en gegevensbron"),
        "Langue de l’application" to mapOf("ar" to "لغة التطبيق", "en" to "App language", "es" to "Idioma de la aplicación", "de" to "App-Sprache", "it" to "Lingua dell’app", "pt" to "Idioma da aplicação", "nl" to "App-taal"),
        "Choisissez votre langue" to mapOf("ar" to "اختر لغتك", "en" to "Choose your language", "es" to "Elige tu idioma", "de" to "Sprache auswählen", "it" to "Scegli la lingua", "pt" to "Escolha o idioma", "nl" to "Kies je taal"),
        "Français" to mapOf("ar" to "الفرنسية", "en" to "French", "es" to "Francés", "de" to "Französisch", "it" to "Francese", "pt" to "Francês", "nl" to "Frans"),
        "English" to mapOf("ar" to "الإنجليزية", "en" to "English", "es" to "Inglés", "de" to "Englisch", "it" to "Inglese", "pt" to "Inglês", "nl" to "Engels"),
        "Español" to mapOf("ar" to "الإسبانية", "en" to "Spanish", "es" to "Español", "de" to "Spanisch", "it" to "Spagnolo", "pt" to "Espanhol", "nl" to "Spaans"),
        "Deutsch" to mapOf("ar" to "الألمانية", "en" to "German", "es" to "Alemán", "de" to "Deutsch", "it" to "Tedesco", "pt" to "Alemão", "nl" to "Duits"),
        "Italiano" to mapOf("ar" to "الإيطالية", "en" to "Italian", "es" to "Italiano", "de" to "Italienisch", "it" to "Italiano", "pt" to "Italiano", "nl" to "Italiaans"),
        "Português" to mapOf("ar" to "البرتغالية", "en" to "Portuguese", "es" to "Portugués", "de" to "Portugiesisch", "it" to "Portoghese", "pt" to "Português", "nl" to "Portugees"),
        "Nederlands" to mapOf("ar" to "الهولندية", "en" to "Dutch", "es" to "Neerlandés", "de" to "Niederländisch", "it" to "Olandese", "pt" to "Neerlandês", "nl" to "Nederlands"),
        "Enregistrer la langue" to mapOf("ar" to "حفظ اللغة", "en" to "Save language", "es" to "Guardar idioma", "de" to "Sprache speichern", "it" to "Salva lingua", "pt" to "Guardar idioma", "nl" to "Taal opslaan"),
    )
    private fun tr(value: String): String = translations[value]?.get(currentLanguage()) ?: value
    private fun text(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = tr(value); textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(3), dp(2), dp(3))
            if (currentLanguage() == "ar") { textDirection = View.TEXT_DIRECTION_RTL; gravity = Gravity.RIGHT }
        }
    }
    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = tr(label); isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
            minHeight = dp(48); setPadding(dp(14), dp(9), dp(14), dp(9))
            maxLines = 2
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply { setColor(green); cornerRadius = dp(14).toFloat() }
            elevation = dp(1).toFloat()
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
            setImageResource(com.scanfoodai.foodscanner.nutrition.R.drawable.ic_scanfood_logo)
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
        val rtl = currentLanguage() == "ar"
        val wide = resources.configuration.smallestScreenWidthDp >= 600
        shell = LinearLayout(this).apply {
            orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setBackgroundColor(pale)
            layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        contentScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(36))
            clipToPadding = false
            setBackgroundColor(pale)
            layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        contentScroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        if (wide) {
            shell.addView(createNavigation(true), LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.MATCH_PARENT))
            shell.addView(contentScroll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        } else {
            shell.addView(contentScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            shell.addView(createNavigation(false), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        // Apply system-bar insets to the shell so bottom navigation never sits behind Android's system buttons.
        shell.setOnApplyWindowInsetsListener { view, insets ->
            val bottomInset = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.navigationBars() or android.view.WindowInsets.Type.displayCutout()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            val topInset = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.displayCutout()).top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            view.setPadding(0, topInset, 0, if (wide) 0 else bottomInset)
            insets
        }
        setContentView(shell)
        shell.requestApplyInsets()
        showHome()
    }

    private fun createNavigation(sidebar: Boolean): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = if (sidebar) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = if (sidebar) Gravity.TOP else Gravity.CENTER
            setPadding(if (sidebar) dp(12) else dp(3), if (sidebar) dp(8) else dp(5), if (sidebar) dp(12) else dp(3), if (sidebar) dp(8) else dp(5))
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        if (sidebar) {
            nav.addView(text("🥑 ScanFood AI", 20f, green, true).apply {
                setPadding(dp(8), dp(18), dp(8), dp(24))
            })
        }
        val items = listOf(
            Triple("⌂", "Accueil") { showHome() },
            Triple("▣", "Scanner") { scan() },
            Triple("◷", "Historique") { showHistory() },
            Triple("♡", "Favoris") { showFavorites() },
            Triple("•••", "Plus") { showMore() }
        )
        items.forEach { item ->
            val col = LinearLayout(this).apply {
                orientation = if (sidebar) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(if (sidebar) dp(8) else dp(1), if (sidebar) dp(12) else dp(1), if (sidebar) dp(8) else dp(1), if (sidebar) dp(12) else dp(1))
                minimumHeight = if (sidebar) dp(48) else dp(42)
                if (sidebar) background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(246, 251, 247)); cornerRadius = dp(12).toFloat()
                }
            }
            col.addView(text(item.first, if (sidebar) 21f else 17f, green, true).apply { gravity = Gravity.CENTER; includeFontPadding = false })
            col.addView(text(item.second, if (sidebar) 14f else 9f, if (sidebar) ink else muted, true).apply {
                gravity = if (sidebar) Gravity.CENTER_VERTICAL else Gravity.CENTER
                maxLines = 1
                includeFontPadding = false
                ellipsize = android.text.TextUtils.TruncateAt.END
                if (sidebar) setPadding(dp(12), 0, dp(4), 0)
            })
            col.setOnClickListener { item.third.invoke() }
            nav.addView(col, if (sidebar) LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(5) } else LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return nav
    }
    private fun resetContent() {
        root.removeAllViews()
        contentScroll.scrollTo(0, 0)
    }
    private fun showHome() {
        resetContent()

        // Dashboard header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        brand.addView(text("🥑 ScanFood AI", 26f, green, true))
        brand.addView(text("Scannez • Comprenez • Choisissez mieux", 13f, muted))
        header.addView(brand)
        header.addView(button("⚙") { showSettings() }.apply {
            textSize = 18f
            minWidth = dp(48)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            contentDescription = "Paramètres et langue"
        })
        root.addView(header, margin())

        // Main call-to-action card
        val hero = card().apply {
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 89, 63), Color.rgb(8, 127, 91), Color.rgb(25, 160, 105))
            ).apply { cornerRadius = dp(24).toFloat() }
            elevation = dp(3).toFloat()
        }
        hero.addView(text("Mangez plus malin 🌿", 24f, Color.WHITE, true))
        hero.addView(text("Scannez un code-barres pour consulter les informations nutritionnelles, les ingrédients et les allergènes.", 14f, Color.WHITE).apply {
            setLineSpacing(dp(3).toFloat(), 1.05f)
        }, margin())
        hero.addView(button("📷  Scanner un produit", ::scan).apply {
            textSize = 16f
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(15).toFloat()
            }
            setTextColor(green)
        }, margin())
        root.addView(hero, margin())

        // Search area is visually separated from the primary scan action.
        val searchCard = card()
        searchCard.addView(text("Recherche par nom ou code", 18f, ink, true))
        searchCard.addView(text("Cherchez une marque, un aliment ou saisissez le code-barres.", 13f, muted), margin())
        searchInput = styleInput(EditText(this).apply {
            hint = "Nom du produit, marque ou code-barres"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { search(); true } else false
            }
        })
        searchCard.addView(searchInput, margin())
        searchCard.addView(button("🔎  Rechercher un produit", ::search), margin())
        root.addView(searchCard, margin())

        status = text("Prêt à scanner. Internet requis pour les fiches en ligne.", 13f, muted)
        root.addView(status, margin())

        // Useful actions are grouped together instead of being scattered through the page.
        root.addView(text("Vos outils", 20f, ink, true), margin())
        val toolsGrid = android.widget.GridLayout(this).apply {
            columnCount = if (resources.configuration.smallestScreenWidthDp >= 600) 4 else 2
            useDefaultMargins = false
        }
        fun toolTile(icon: String, title: String, subtitle: String, action: () -> Unit) {
            val tile = card().apply {
                setPadding(dp(13), dp(14), dp(13), dp(14))
                elevation = dp(1).toFloat()
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.WHITE); cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), Color.rgb(225, 237, 229))
                }
            }
            tile.addView(text(icon, 25f, green, true))
            tile.addView(text(title, 15f, ink, true), margin())
            tile.addView(text(subtitle, 12f, muted))
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            toolsGrid.addView(tile, params)
        }
        toolTile("⚖️", "Comparer", "Comparer deux produits") { showCompare() }
        toolTile("❤️", "Favoris", "Produits enregistrés") { showFavorites() }
        toolTile("🕘", "Historique", "Produits consultés") { showHistory() }
        toolTile("🌐", "Langue", "Modifier la langue") { showSettings() }
        root.addView(toolsGrid, margin())

        root.addView(text("Vos derniers produits", 20f, ink, true), margin())
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(resultBox)
        showSavedInto("history", resultBox)
        root.addView(text("Les données proviennent d’Open Food Facts et peuvent être incomplètes. Elles ne remplacent pas un avis médical.", 12f, muted), margin())
    }
    private fun showMore() {
        resetContent()
        root.addView(text("Plus de ScanFood AI", 26f, green, true))
        root.addView(text("Tous vos outils au même endroit", 14f, muted))
        listOf(
            "⚖️ Comparer des produits" to { showCompare() },
            "⚙️ À propos et source des données" to { showSettings() }
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
    private fun showSettings() {
        resetContent()
        root.addView(text("⚙️ Paramètres", 26f, green, true))
        root.addView(text("Langue et informations de l’application", 14f, muted))
        val languageCard = card()
        languageCard.addView(text("Langue de l’application", 18f, ink, true))
        languageCard.addView(text("Choisissez votre langue", 13f, muted))
        val names = listOf("Français", "English", "العربية", "Español", "Deutsch", "Italiano", "Português", "Nederlands")
        val codes = listOf("fr", "en", "ar", "es", "de", "it", "pt", "nl")
        val selector = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            setSelection(codes.indexOf(currentLanguage()).coerceAtLeast(0))
        }
        languageCard.addView(selector, margin())
        languageCard.addView(button("Enregistrer la langue") {
            prefs.edit().putString("language_code", codes[selector.selectedItemPosition]).apply()
            Toast.makeText(this, "Langue mise à jour", Toast.LENGTH_SHORT).show()
            buildHome()
        }, margin())
        root.addView(languageCard, margin())
        val aboutCard = card()
        aboutCard.addView(text("À propos", 18f, ink, true))
        aboutCard.addView(text("ScanFood AI utilise les informations disponibles dans Open Food Facts. Les fiches peuvent être incomplètes ou inexactes.", 13f, muted))
        aboutCard.addView(text("Les informations nutritionnelles sont indicatives et ne remplacent pas un avis médical.", 13f, muted), margin())
        root.addView(aboutCard, margin())
    }
    private fun margin(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
    private fun scan() {
        try {
            com.google.zxing.integration.android.IntentIntegrator(this)
                .setPrompt("Place le code-barres dans le cadre")
                .setBeepEnabled(false)
                .setCameraId(0)
                .setOrientationLocked(false)
                .setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.ALL_CODE_TYPES)
                .initiateScan()
        } catch (e: Exception) {
            if (::status.isInitialized) status.text = "Impossible d’ouvrir le scanner : ${e.message ?: "erreur"}"
            else Toast.makeText(this, "Impossible d’ouvrir le scanner.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Legacy scanner result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            val code = result.contents
            buildHome()
            if (code.isNullOrBlank()) {
                status.text = "Scan annulé."
            } else {
                searchInput.setText(code)
                search()
            }
        }
    }
    private fun search() {
        val q = searchInput.text.toString().trim()
        if (q.isEmpty()) { searchInput.error = "Saisis un nom ou scanne un code"; return }
        status.text = "ScanFood AI"
        resultBox.removeAllViews()
        val loadingCard = card()
        loadingCard.orientation = LinearLayout.HORIZONTAL
        loadingCard.gravity = Gravity.CENTER_VERTICAL
        loadingCard.addView(ProgressBar(this).apply { isIndeterminate = true }, LinearLayout.LayoutParams(dp(34), dp(34)))
        loadingCard.addView(text("  Recherche des informations nutritionnelles…", 14f, green, true))
        resultBox.addView(loadingCard, margin())
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
