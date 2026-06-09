package com.mbzerker.cadernoreceitas;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;

public class MainActivity extends Activity {
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/MBZerker/CadernoReceitas/main/docs/update.json";
    private static final int RED = Color.rgb(184, 50, 22);
    private static final int RED_DARK = Color.rgb(127, 29, 18);
    private static final int GOLD = Color.rgb(217, 154, 59);
    private static final int PAPER = Color.rgb(255, 247, 237);
    private static final int INK = Color.rgb(47, 27, 18);
    private static final int MUTED = Color.rgb(118, 88, 72);
    private static final int CARD = Color.argb(224, 255, 249, 236);
    private static final int CARD_STRONG = Color.argb(242, 255, 247, 237);
    private static final int LINE = Color.rgb(232, 201, 142);
    private static final int LINK = Color.rgb(53, 99, 199);

    private Db db;
    private LinearLayout root;
    private LinearLayout listArea;
    private EditText search;
    private final ArrayDeque<NavState> backStack = new ArrayDeque<>();
    private final Handler quizHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<QuizQuestion> quizQuestions = new ArrayList<>();
    private Runnable quizTick;
    private TextView quizTimer;
    private int quizIndex;
    private int quizScore;
    private long quizDeadline;
    private boolean quizGrace;
    private int currentCadernoId;
    private int currentCategoriaId;
    private int currentReceitaId;
    private String screen = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        configureSystemBars();
        db = new Db(this);
        db.getWritableDatabase();
        showSplash();
    }

    private void showSplash() {
        ImageView splash = new ImageView(this);
        splash.setImageResource(R.drawable.splash_full);
        splash.setScaleType(ImageView.ScaleType.CENTER_CROP);
        setContentView(splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::showHome, 1800);
    }

    private void base(int background) {
        stopQuizTimer();
        configureSystemBars();
        FrameLayout frame = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(background);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(220));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), statusBarHeight() + dp(14), dp(16), dp(120));
        scroll.addView(root);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        frame.addView(statusBarShield(), new FrameLayout.LayoutParams(-1, statusBarHeight(), Gravity.TOP));
        setContentView(frame);
    }

    private void showHome() {
        screen = "home";
        currentCadernoId = 0;
        base(R.drawable.bg_principal);

        LinearLayout logoFrame = logoCard();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_caderno);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoFrame.addView(logo, new LinearLayout.LayoutParams(-1, dp(120)));
        root.addView(logoFrame);

        LinearLayout actions = card();
        actions.addView(titleRow(R.drawable.ic_book, "Caderno de Receitas", 26));
        actions.addView(label("Organize cadernos, grupos, receitas e ingredientes.", 14, MUTED, false));
        LinearLayout row = iconStrip();
        addWeightedStripIcon(row, R.drawable.ic_plus, RED, "Novo caderno", v -> newCaderno());
        addWeightedStripIcon(row, R.drawable.ic_update, GOLD, "Atualizar", v -> checkUpdate());
        actions.addView(row, actionStripParams());
        root.addView(actions);

        addSearch("Pesquisar cadernos", this::renderHomeList);
        listArea = new LinearLayout(this);
        listArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(listArea);
        renderHomeList();
    }

    private void renderHomeList() {
        listArea.removeAllViews();
        List<Item> items = db.cadernos(text(search));
        if (items.isEmpty()) {
            listArea.addView(empty("Nenhum caderno criado.", "Toque em + para criar o primeiro caderno."));
            return;
        }
        for (Item c : items) {
            LinearLayout card = itemCard(R.drawable.ic_book, c.name, c.desc, db.countReceitasCaderno(c.id) + " receitas", c.locked, () -> showCaderno(c.id), () -> toggleLock("cadernos", c, this::renderHomeList), () -> menuCaderno(c));
            card.setOnLongClickListener(v -> { menuCaderno(c); return true; });
            listArea.addView(card);
        }
    }

    private void showCaderno(int id) {
        screen = "caderno";
        currentCadernoId = id;
        Item caderno = db.get("cadernos", id);
        base(R.drawable.bg_caderno);
        root.addView(header(R.drawable.ic_book, caderno.name, caderno.desc, this::showHome));

        LinearLayout add = card();
        add.addView(titleRow(R.drawable.ic_category, "Grupo", 20));
        add.addView(label("Crie grupos para separar massas, doces, molhos e outros preparos.", 14, MUTED, false));
        LinearLayout cadernoActions = iconStrip();
        addWeightedStripIcon(cadernoActions, R.drawable.ic_plus, RED, "Adicionar grupo", v -> newCategoria());
        addWeightedStripIcon(cadernoActions, R.drawable.ic_clipboard_list, GOLD, "Ingredientes cadastrados", v -> showIngredientesCaderno());
        addWeightedStripIcon(cadernoActions, R.drawable.ic_report, RED_DARK, "Teste", v -> startQuizOrExplain());
        add.addView(cadernoActions, actionStripParams());
        root.addView(add);

        addSearch("Pesquisar grupos", this::renderCategorias);
        listArea = new LinearLayout(this);
        listArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(listArea);
        renderCategorias();
    }

    private void renderCategorias() {
        listArea.removeAllViews();
        List<Item> items = db.categorias(currentCadernoId, text(search));
        if (items.isEmpty()) {
            listArea.addView(empty("Nenhum grupo.", "Crie um grupo para organizar receitas."));
            return;
        }
        for (Item item : items) {
            LinearLayout card = itemCard(R.drawable.ic_category, item.name, item.desc, db.countReceitasCategoria(item.id) + " receitas", item.locked, () -> showCategoria(item.id), () -> toggleLock("categorias", item, this::renderCategorias), () -> menuCategoria(item));
            card.setOnLongClickListener(v -> { menuCategoria(item); return true; });
            listArea.addView(card);
        }
    }

    private void showIngredientesCaderno() {
        screen = "ingredientes_caderno";
        base(R.drawable.bg_ingredientes);
        root.addView(header(R.drawable.ic_clipboard_list, "Ingredientes cadastrados", "Todos os ingredientes deste caderno.", () -> showCaderno(currentCadernoId)));
        addSearch("Pesquisar ingredientes", this::renderIngredientesCaderno);
        listArea = new LinearLayout(this);
        listArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(listArea);
        renderIngredientesCaderno();
    }

    private void renderIngredientesCaderno() {
        listArea.removeAllViews();
        List<Item> items = db.ingredientesCaderno(currentCadernoId, text(search));
        if (items.isEmpty()) {
            listArea.addView(empty("Nenhum ingrediente cadastrado.", "Os ingredientes aparecem aqui conforme forem adicionados nas receitas."));
            return;
        }
        for (Item item : items) {
            LinearLayout card = itemCard(item.recipeLinkId > 0 ? R.drawable.ic_link : R.drawable.ic_ingredient, item.name, item.desc, item.extra, item.locked, () -> showReceita(item.parentId), () -> toggleLock("ingredientes", item, this::renderIngredientesCaderno), () -> menuIngredienteCatalogo(item));
            if (item.recipeLinkId > 0) markLinkedIngredient(card);
            card.setOnLongClickListener(v -> { menuIngredienteCatalogo(item); return true; });
            listArea.addView(card);
        }
    }

    private void showCategoria(int id) {
        screen = "categoria";
        currentCategoriaId = id;
        Item cat = db.get("categorias", id);
        currentCadernoId = cat.parentId;
        base(R.drawable.bg_receitas);
        root.addView(header(R.drawable.ic_category, cat.name, cat.desc, () -> showCaderno(currentCadernoId)));

        LinearLayout add = card();
        add.addView(titleRow(R.drawable.ic_recipe, "Receita", 20));
        add.addView(label("Cadastre receitas deste grupo.", 14, MUTED, false));
        addActionButton(add, R.drawable.ic_plus, "Adicionar receita", v -> newReceita());
        root.addView(add);

        addSearch("Pesquisar receitas", this::renderReceitas);
        listArea = new LinearLayout(this);
        listArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(listArea);
        renderReceitas();
    }

    private void renderReceitas() {
        listArea.removeAllViews();
        List<Item> items = db.receitas(currentCategoriaId, text(search));
        if (items.isEmpty()) {
            listArea.addView(empty("Nenhuma receita.", "Adicione a primeira receita deste grupo."));
            return;
        }
        for (Item item : items) {
            LinearLayout card = itemCard(R.drawable.ic_recipe, item.name, item.desc, db.countIngredientes(item.id) + " ingredientes", item.locked, () -> {
                if (item.locked) showRecipePreview(item.id);
                else showReceita(item.id);
            }, () -> toggleLock("receitas", item, this::renderReceitas), () -> menuReceita(item));
            card.setOnLongClickListener(v -> { menuReceita(item); return true; });
            listArea.addView(card);
        }
    }

    private void showReceita(int id) {
        screen = "receita";
        currentReceitaId = id;
        Item receita = db.getReceita(id);
        currentCategoriaId = receita.parentId;
        currentCadernoId = receita.cadernoId;
        base(R.drawable.bg_ingredientes);

        root.addView(header(R.drawable.ic_recipe, receita.name, "Ingredientes e preparo da receita.", this::backFromReceita));

        LinearLayout ingredientActions = card();
        ingredientActions.addView(titleRow(R.drawable.ic_ingredient, "Ingredientes", 20));
        ingredientActions.addView(label("Adicione os ingredientes desta receita.", 14, MUTED, false));
        addActionButton(ingredientActions, R.drawable.ic_plus, "Adicionar ingrediente", v -> newIngrediente());
        root.addView(ingredientActions);

        int ingredientCount = db.countIngredientes(id);
        if (ingredientCount >= 2) {
            LinearLayout preparoCard = card();
            preparoCard.addView(titleRow(R.drawable.ic_prep, "Modo de preparo", 20));
            preparoCard.addView(label(receita.desc.isEmpty() ? "Toque no lapis para cadastrar o modo de preparo." : receita.desc, 15, INK, false));
            addActionButton(preparoCard, R.drawable.ic_edit, "Editar modo de preparo", v -> editPreparo(receita));
            root.addView(preparoCard);
        }

        addSearch("Pesquisar ingredientes", this::renderIngredientes);
        listArea = new LinearLayout(this);
        listArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(listArea);
        renderIngredientes();
    }

    private void renderIngredientes() {
        listArea.removeAllViews();
        List<Item> items = db.ingredientes(currentReceitaId, text(search));
        if (items.isEmpty()) {
            listArea.addView(empty("Nenhum ingrediente.", "Cadastre os ingredientes desta receita."));
            return;
        }
        for (Item item : items) {
            LinearLayout card = itemCard(item.recipeLinkId > 0 ? R.drawable.ic_link : R.drawable.ic_ingredient, item.name, item.desc, item.extra, item.locked, item.recipeLinkId > 0 ? () -> openLinkedReceita(item.recipeLinkId) : null, () -> toggleLock("ingredientes", item, this::renderIngredientes), () -> menuIngrediente(item));
            if (item.recipeLinkId > 0) {
                markLinkedIngredient(card);
            }
            card.setOnLongClickListener(v -> { menuIngrediente(item); return true; });
            listArea.addView(card);
        }
    }

    private void startQuizOrExplain() {
        int total = db.countReceitasCaderno(currentCadernoId);
        if (total < 5) {
            toast("Teste bloqueado: cadastre pelo menos 5 receitas neste caderno.");
            return;
        }
        quizQuestions.clear();
        quizQuestions.addAll(buildQuizQuestions());
        if (quizQuestions.size() < 5) {
            showThemed(themedDialog("Teste bloqueado", null)
                .setMessage("Cadastre ingredientes e modo de preparo em mais receitas para gerar perguntas realmente dificeis.")
                .setPositiveButton("Entendi", null));
            return;
        }
        Collections.shuffle(quizQuestions);
        while (quizQuestions.size() > 12) quizQuestions.remove(quizQuestions.size() - 1);
        quizIndex = 0;
        quizScore = 0;
        showQuizQuestion();
    }

    private ArrayList<QuizQuestion> buildQuizQuestions() {
        ArrayList<QuizQuestion> out = new ArrayList<>();
        Random random = new Random();
        List<Item> recipes = db.receitasCaderno(currentCadernoId);
        ArrayList<String> allIngredients = new ArrayList<>();
        HashMap<Integer, List<Item>> ingredientsByRecipe = new HashMap<>();
        for (Item recipe : recipes) {
            List<Item> ingredients = db.ingredientes(recipe.id, "");
            ingredientsByRecipe.put(recipe.id, ingredients);
            for (Item ingredient : ingredients) addUnique(allIngredients, ingredient.name);
        }

        for (Item recipe : recipes) {
            List<Item> ingredients = ingredientsByRecipe.get(recipe.id);
            if (ingredients == null || ingredients.size() < 2) continue;
            ArrayList<String> recipeIngredients = ingredientNames(ingredients);
            String correctIngredient = recipeIngredients.get(random.nextInt(recipeIngredients.size()));
            ArrayList<String> distractors = withoutNormalized(allIngredients, recipeIngredients);
            addQuestion(out, "Na receita \"" + recipe.name + "\", qual ingrediente parece plausivel, mas e o verdadeiro detalhe da ficha?", correctIngredient, pickDistractors(distractors, correctIngredient, 3));

            if (recipeIngredients.size() >= 3 && !distractors.isEmpty()) {
                String intruder = distractors.get(random.nextInt(distractors.size()));
                ArrayList<String> wrong = pickDistractors(recipeIngredients, intruder, 3);
                addQuestion(out, "Qual alternativa NAO pertence a \"" + recipe.name + "\"?", intruder, wrong);
            }

            if (recipeIngredients.size() >= 2) {
                Collections.shuffle(recipeIngredients);
                String a = recipeIngredients.get(0);
                String b = recipeIngredients.get(1);
                ArrayList<String> recipeNames = new ArrayList<>();
                for (Item other : recipes) {
                    if (other.id != recipe.id) addUnique(recipeNames, other.name);
                }
                addQuestion(out, "Qual receita usa ao mesmo tempo \"" + a + "\" e \"" + b + "\"?", recipe.name, pickDistractors(recipeNames, recipe.name, 3));
            }

            if (!recipe.desc.isEmpty()) {
                ArrayList<String> prepDistractors = new ArrayList<>();
                ArrayList<String> recipeDistractors = new ArrayList<>();
                for (Item other : recipes) {
                    if (other.id == recipe.id) continue;
                    if (!other.desc.isEmpty()) addUnique(prepDistractors, prepSnippet(other.desc));
                    addUnique(recipeDistractors, other.name);
                }
                addQuestion(out, "Qual trecho de preparo pertence a \"" + recipe.name + "\"?", prepSnippet(recipe.desc), pickDistractors(prepDistractors, prepSnippet(recipe.desc), 3));
                addQuestion(out, "Esta instrucao entrega uma receita, mas tenta parecer generica: \"" + prepSnippet(recipe.desc) + "\". Qual e a receita?", recipe.name, pickDistractors(recipeDistractors, recipe.name, 3));
            }
        }
        return out;
    }

    private void addQuestion(ArrayList<QuizQuestion> out, String prompt, String correct, ArrayList<String> distractors) {
        if (correct == null || correct.trim().isEmpty() || distractors.size() < 3) return;
        ArrayList<String> options = new ArrayList<>();
        options.add(correct);
        for (String value : distractors) {
            if (options.size() == 4) break;
            if (!containsNorm(options, value)) options.add(value);
        }
        if (options.size() < 4) return;
        Collections.shuffle(options);
        out.add(new QuizQuestion(prompt, options, indexOfNorm(options, correct)));
    }

    private void showQuizQuestion() {
        screen = "quiz";
        if (quizIndex >= quizQuestions.size()) {
            showQuizResult();
            return;
        }
        base(R.drawable.bg_quiz);
        QuizQuestion question = quizQuestions.get(quizIndex);

        LinearLayout top = card();
        TextView title = label("Teste do Caderno", 24, RED, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title);
        quizTimer = label("", 18, RED_DARK, true);
        quizTimer.setGravity(Gravity.CENTER);
        top.addView(quizTimer);
        TextView progress = label("Pergunta " + (quizIndex + 1) + " de " + quizQuestions.size() + "  |  Pontos: " + quizScore, 14, MUTED, true);
        progress.setGravity(Gravity.CENTER);
        top.addView(progress);
        root.addView(top);

        LinearLayout questionCard = card();
        TextView q = label(question.prompt, 20, INK, true);
        q.setGravity(Gravity.CENTER);
        questionCard.addView(q);
        root.addView(questionCard);

        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < question.options.size(); i++) {
            final int choice = i;
            root.addView(quizOption(letters[i] + ". " + question.options.get(i), () -> answerQuiz(choice)));
        }
        startQuizTimer();
    }

    private TextView quizOption(String value, Runnable action) {
        TextView option = label(value, 17, INK, true);
        option.setPadding(dp(16), dp(16), dp(16), dp(16));
        option.setBackground(round(CARD_STRONG, dp(18), LINE, 1));
        option.setMinHeight(dp(68));
        option.setGravity(Gravity.CENTER_VERTICAL);
        option.setOnClickListener(v -> {
            v.setEnabled(false);
            action.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        option.setLayoutParams(params);
        return option;
    }

    private void startQuizTimer() {
        stopQuizTimer();
        quizGrace = false;
        quizDeadline = System.currentTimeMillis() + 30000;
        quizTick = () -> {
            long left = quizDeadline - System.currentTimeMillis();
            if (left < 0) {
                if (!quizGrace) {
                    quizGrace = true;
                    quizDeadline = System.currentTimeMillis() + 10000;
                    left = 10000;
                } else {
                    showGameOver("Tempo esgotado.");
                    return;
                }
            }
            if (quizTimer != null) {
                long seconds = Math.max(0, (left + 999) / 1000);
                quizTimer.setText(quizGrace ? "Acrescimo: " + seconds + "s" : "Tempo: " + seconds + "s");
            }
            quizHandler.postDelayed(quizTick, 250);
        };
        quizHandler.post(quizTick);
    }

    private void stopQuizTimer() {
        if (quizTick != null) quizHandler.removeCallbacks(quizTick);
        quizTick = null;
    }

    private void answerQuiz(int choice) {
        stopQuizTimer();
        QuizQuestion question = quizQuestions.get(quizIndex);
        if (choice != question.correctIndex) {
            showGameOver("Resposta incorreta.");
            return;
        }
        quizScore += quizGrace ? 5 : 10;
        quizIndex++;
        showQuizQuestion();
    }

    private void showQuizResult() {
        stopQuizTimer();
        screen = "quiz_result";
        base(R.drawable.bg_quiz);
        LinearLayout result = card();
        TextView title = label("Resultado", 26, RED, true);
        title.setGravity(Gravity.CENTER);
        result.addView(title);
        TextView score = label("Pontuacao final: " + quizScore + " pontos", 22, INK, true);
        score.setGravity(Gravity.CENTER);
        result.addView(score);
        TextView detail = label("Voce passou pelo teste sem cair nas pegadinhas.", 15, MUTED, false);
        detail.setGravity(Gravity.CENTER);
        result.addView(detail);
        LinearLayout row = iconStrip();
        addWeightedStripIcon(row, R.drawable.ic_back, RED, "Voltar", v -> showCaderno(currentCadernoId));
        addWeightedStripIcon(row, R.drawable.ic_report, RED_DARK, "Novo teste", v -> startQuizOrExplain());
        result.addView(row, actionStripParams());
        root.addView(result);
    }

    private void showGameOver(String reason) {
        stopQuizTimer();
        screen = "game_over";
        configureSystemBars();
        FrameLayout frame = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.game_over);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setPadding(dp(14), statusBarHeight() + dp(14), dp(14), dp(18));
        ImageButton back = imageIconButton(R.drawable.ic_back, RED, Color.WHITE);
        back.setContentDescription("Voltar");
        back.setOnClickListener(v -> showCaderno(currentCadernoId));
        overlay.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        Space spacer = new Space(this);
        overlay.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        LinearLayout card = card();
        TextView title = label("Game over", 28, RED, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView msg = label(reason + " Pontuacao: " + quizScore + " pontos.", 16, INK, true);
        msg.setGravity(Gravity.CENTER);
        card.addView(msg);
        overlay.addView(card);
        frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        frame.addView(statusBarShield(), new FrameLayout.LayoutParams(-1, statusBarHeight(), Gravity.TOP));
        setContentView(frame);
    }

    private void showRecipePreview(int id) {
        screen = "recipe_preview";
        currentReceitaId = id;
        Item receita = db.getReceita(id);
        currentCategoriaId = receita.parentId;
        currentCadernoId = receita.cadernoId;
        configureSystemBars();

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(PAPER);
        LinearLayout screenView = new LinearLayout(this);
        screenView.setOrientation(LinearLayout.VERTICAL);
        screenView.setPadding(dp(14), statusBarHeight() + dp(14), dp(14), dp(18));

        addPrintPreviewToolbar(screenView, () -> showCategoria(currentCategoriaId), () -> printHtml("Receita - " + receita.name, buildRecipePrintHtml(receita)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(220));
        LinearLayout page = printPage();
        fillRecipePreview(page, receita);
        int pageWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(430));
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(pageWidth, -2);
        pageParams.gravity = Gravity.CENTER_HORIZONTAL;
        pageParams.setMargins(0, dp(12), 0, dp(40));
        scroll.addView(page, pageParams);
        screenView.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        frame.addView(screenView, new FrameLayout.LayoutParams(-1, -1));
        frame.addView(statusBarShield(), new FrameLayout.LayoutParams(-1, statusBarHeight(), Gravity.TOP));
        setContentView(frame);
    }

    private void addPrintPreviewToolbar(LinearLayout screenView, Runnable closeAction, Runnable printAction) {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = imageIconButton(R.drawable.ic_back, RED, Color.WHITE);
        back.setContentDescription("Voltar");
        back.setOnClickListener(v -> closeAction.run());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        View spacer = new View(this);
        toolbar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        ImageButton print = imageIconButton(R.drawable.ic_print, RED_DARK, Color.WHITE);
        print.setContentDescription("Imprimir");
        print.setOnClickListener(v -> printAction.run());
        toolbar.addView(print, new LinearLayout.LayoutParams(dp(48), dp(48)));
        screenView.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private LinearLayout printPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(28), dp(32), dp(28), dp(32));
        page.setBackgroundColor(Color.WHITE);
        page.setMinimumHeight(dp(560));
        return page;
    }

    private void fillRecipePreview(LinearLayout page, Item receita) {
        TextView title = printText(receita.name, 24, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, matchWrap());

        List<Item> ingredients = db.ingredientes(receita.id, "");
        page.addView(printText("Ingredientes", 18, true), matchWrapWithTop(dp(24)));
        if (ingredients.isEmpty()) {
            page.addView(printText("Nenhum ingrediente cadastrado.", 15, false), matchWrapWithTop(dp(6)));
        } else {
            for (Item ingredient : ingredients) {
                page.addView(printText("* " + ingredientLine(ingredient), 15, false), matchWrapWithTop(dp(7)));
            }
        }

        page.addView(printText("Modo de preparo", 18, true), matchWrapWithTop(dp(24)));
        page.addView(printText(receita.desc.isEmpty() ? "Modo de preparo nao cadastrado." : receita.desc, 15, false), matchWrapWithTop(dp(7)));
    }

    private TextView printText(String value, int size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value == null ? "" : value);
        text.setTextColor(Color.BLACK);
        text.setTextSize(size);
        text.setLineSpacing(0, 1.16f);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private String ingredientLine(Item ingredient) {
        StringBuilder line = new StringBuilder();
        if (!ingredient.desc.isEmpty()) line.append(ingredient.desc).append(" - ");
        line.append(ingredient.name);
        if (!ingredient.extra.isEmpty()) line.append(" (").append(ingredient.extra).append(")");
        if (ingredient.recipeLinkId > 0) line.append(" - receita vinculada");
        return line.toString();
    }

    private void printHtml(String jobName, String html) {
        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (manager == null) {
            toast("Impressao indisponivel neste aparelho.");
            return;
        }
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName);
                PrintAttributes attributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                manager.print(jobName, adapter, attributes);
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private String buildRecipePrintHtml(Item receita) {
        StringBuilder html = printHtmlStart(receita.name);
        html.append("<h1>").append(escapeHtml(receita.name)).append("</h1>");
        html.append("<h2>Ingredientes</h2>");
        List<Item> ingredients = db.ingredientes(receita.id, "");
        if (ingredients.isEmpty()) {
            html.append("<p>Nenhum ingrediente cadastrado.</p>");
        } else {
            for (Item ingredient : ingredients) {
                html.append("<p class=\"item\">&bull; ")
                        .append(escapeHtml(ingredientLine(ingredient)))
                        .append("</p>");
            }
        }
        html.append("<h2>Modo de preparo</h2><p>")
                .append(escapeHtml(receita.desc.isEmpty() ? "Modo de preparo nao cadastrado." : receita.desc).replace("\n", "<br>"))
                .append("</p></body></html>");
        return html.toString();
    }

    private StringBuilder printHtmlStart(String title) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>")
                .append(escapeHtml(title))
                .append("</title><style>")
                .append("@page{size:A4;margin:18mm;}body{font-family:Arial,sans-serif;color:#000;background:#fff;font-size:14pt;line-height:1.35;}")
                .append("h1{text-align:center;font-size:22pt;margin:0 0 18pt;}h2{font-size:17pt;margin:20pt 0 8pt;}")
                .append("p{margin:4pt 0;}.item{margin:0 0 10pt;}span{font-size:13pt;}")
                .append("</style></head><body>");
        return html;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void newCaderno() {
        LinearLayout box = themedDialogBox();
        AutoCompleteTextView nome = autoEntry("Nome do caderno", db.cadernoNames());
        EditText desc = entry("Descricao curta", "");
        box.addView(nome);
        box.addView(desc);
        showThemed(themedDialog("Novo caderno", box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(nome)) return;
                db.saveCaderno(0, text(nome), text(desc));
                renderHomeList();
            }));
    }

    private void newCategoria() {
        LinearLayout box = themedDialogBox();
        EditText nome = entry("Nome do grupo", "");
        EditText desc = entry("Descricao curta", "");
        box.addView(nome);
        box.addView(desc);
        showThemed(themedDialog("Novo grupo", box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(nome)) return;
                db.saveCategoria(0, currentCadernoId, text(nome), text(desc));
                renderCategorias();
            }));
    }

    private void newReceita() {
        LinearLayout box = themedDialogBox();
        EditText nome = entry("Nome da receita", "");
        box.addView(nome);
        showThemed(themedDialog("Nova receita", box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(nome)) return;
                db.saveReceita(0, currentCadernoId, currentCategoriaId, text(nome), "");
                renderReceitas();
            }));
    }

    private void newIngrediente() {
        showIngredientDialog("Novo ingrediente", null);
    }

    private void menuCaderno(Item item) {
        actions(item.name, new String[]{"Editar", "Excluir"}, which -> {
            if (which == 0) editCaderno(item);
            if (which == 1) confirmDelete("cadernos", item, "caderno", "Excluir este caderno e todo o conteudo?", () -> { db.deleteCaderno(item.id); renderHomeList(); });
        });
    }

    private void menuCategoria(Item item) {
        actions(item.name, new String[]{"Editar", "Excluir"}, which -> {
            if (which == 0) editCategoria(item);
            if (which == 1) confirmDelete("categorias", item, "grupo", "Excluir este grupo e suas receitas?", () -> { db.deleteCategoria(item.id); renderCategorias(); });
        });
    }

    private void menuReceita(Item item) {
        actions(item.name, new String[]{"Editar", "Excluir"}, which -> {
            if (which == 0) editReceita(item);
            if (which == 1) confirmDelete("receitas", item, "receita", "Excluir esta receita e seus ingredientes?", () -> { db.deleteReceita(item.id); renderReceitas(); });
        });
    }

    private void menuIngrediente(Item item) {
        actions(item.name, new String[]{"Editar", "Excluir"}, which -> {
            if (which == 0) editIngrediente(item);
            if (which == 1) confirmDelete("ingredientes", item, "ingrediente", "Excluir este ingrediente?", () -> { db.delete("ingredientes", item.id); renderIngredientes(); });
        });
    }

    private void menuIngredienteCatalogo(Item item) {
        actions(item.name, new String[]{"Abrir receita", "Editar", "Excluir"}, which -> {
            if (which == 0) showReceita(item.parentId);
            if (which == 1) {
                currentReceitaId = item.parentId;
                editIngrediente(item);
            }
            if (which == 2) confirmDelete("ingredientes", item, "ingrediente", "Excluir este ingrediente?", () -> { db.delete("ingredientes", item.id); renderIngredientesCaderno(); });
        });
    }

    private void editCaderno(Item item) {
        editTwo("Editar caderno", "Nome", item.name, "Descricao", item.desc, (a, b) -> {
            db.saveCaderno(item.id, a, b);
            renderHomeList();
        });
    }

    private void editCategoria(Item item) {
        editTwo("Editar grupo", "Nome", item.name, "Descricao", item.desc, (a, b) -> {
            db.saveCategoria(item.id, currentCadernoId, a, b);
            renderCategorias();
        });
    }

    private void editReceita(Item item) {
        editOne("Editar receita", "Nome", item.name, value -> {
            db.saveReceita(item.id, currentCadernoId, currentCategoriaId, value, item.desc);
            if ("receita".equals(screen)) showReceita(item.id);
            else renderReceitas();
        });
    }

    private void editPreparo(Item item) {
        LinearLayout box = themedDialogBox();
        MultiAutoCompleteTextView preparo = prepEntry("Modo de preparo", item.desc, db.ingredientNamesForReceita(item.id));
        box.addView(preparo);
        showThemed(themedDialog("Modo de preparo", box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                db.saveReceita(item.id, currentCadernoId, currentCategoriaId, item.name, text(preparo));
                showReceita(item.id);
            }));
    }

    private void editIngrediente(Item item) {
        showIngredientDialog("Editar ingrediente", item);
    }

    private void showIngredientDialog(String title, Item item) {
        LinearLayout box = themedDialogBox();
        AutoCompleteTextView nome = autoEntry("Nome do ingrediente", db.ingredientAndRecipeNames(currentReceitaId));
        EditText qtd = entry("Quantidade", "");
        AutoCompleteTextView cat = autoEntry(categoryHintFor(""), db.ingredientCategories());
        if (item != null) {
            nome.setText(item.name);
            qtd.setText(item.desc);
            cat.setText(item.extra);
        }
        nome.addTextChangedListener(new SimpleWatcher() {
            public void afterTextChanged(Editable s) {
                if (text(cat).isEmpty()) cat.setHint(categoryHintFor(text(nome)));
            }
        });
        box.addView(nome);
        box.addView(qtd);
        box.addView(cat);
        showThemed(themedDialog(title, box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(nome)) return;
                int recipeLink = db.findRecipeByName(text(nome), currentReceitaId);
                String categoria = text(cat);
                if (recipeLink > 0 && categoria.isEmpty()) categoria = "Receita preparada";
                db.saveIngrediente(item == null ? 0 : item.id, currentReceitaId, text(nome), text(qtd), categoria, recipeLink);
                showReceita(currentReceitaId);
            }));
    }

    private void checkUpdate() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Verificando atualizacao...");
        dialog.setIndeterminate(true);
        dialog.show();
        styleDialogWindow(dialog);
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(readUrl(UPDATE_URL));
                int remote = json.optInt("versionCode", 0);
                String version = json.optString("versionName", "");
                if (remote <= BuildConfig.VERSION_CODE) {
                    ui(() -> { dialog.dismiss(); toast("Sem atualizacao disponivel."); });
                    return;
                }
                ui(() -> {
                    dialog.dismiss();
                    confirm("Atualizacao disponivel", "Baixar versao " + version + " agora?", () -> downloadApk(json));
                });
            } catch (Exception e) {
                ui(() -> { dialog.dismiss(); toast("Falha ao verificar atualizacao."); });
            }
        }).start();
    }

    private void downloadApk(JSONObject manifest) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Baixando atualizacao");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
        dialog.show();
        styleDialogWindow(dialog);
        new Thread(() -> {
            try {
                String apkUrl = manifest.getString("apkUrl");
                String expected = manifest.optString("sha256", "");
                File out = new File(getExternalCacheDir(), "CadernoReceitas-update.apk");
                URL url = new URL(apkUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.connect();
                int total = Math.max(1, con.getContentLength());
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                try (InputStream in = con.getInputStream(); OutputStream os = new FileOutputStream(out)) {
                    byte[] buffer = new byte[96 * 1024];
                    int read;
                    long done = 0;
                    while ((read = in.read(buffer)) >= 0) {
                        os.write(buffer, 0, read);
                        sha.update(buffer, 0, read);
                        done += read;
                        int progress = (int) Math.min(100, (done * 100) / total);
                        ui(() -> dialog.setProgress(progress));
                    }
                }
                String hash = bytesToHex(sha.digest());
                if (!expected.isEmpty() && !hash.equalsIgnoreCase(expected)) {
                    throw new IOException("SHA-256 diferente do manifesto.");
                }
                ui(() -> { dialog.dismiss(); openApk(out); });
            } catch (Exception e) {
                ui(() -> { dialog.dismiss(); toast("Falha ao baixar atualizacao."); });
            }
        }).start();
    }

    private void openApk(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivity(settings);
        }
    }

    private LinearLayout header(int icon, String title, String subtitle, Runnable back) {
        LinearLayout box = card();
        box.addView(headerInline(title, back));
        if (!subtitle.isEmpty()) box.addView(label(subtitle, 14, MUTED, false));
        return box;
    }

    private void markLinkedIngredient(LinearLayout card) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235, 245, 250, 255));
        bg.setStroke(dp(1), LINK);
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
    }

    private LinearLayout headerInline(String title, Runnable back) {
        LinearLayout row = hrow();
        ImageButton backButton = imageIconButton(R.drawable.ic_back, RED, Color.WHITE);
        backButton.setContentDescription("Voltar");
        backButton.setOnClickListener(v -> back.run());
        row.addView(backButton, new LinearLayout.LayoutParams(dp(56), dp(56)));
        TextView titleText = label(title, 24, RED, true);
        titleText.setGravity(Gravity.CENTER);
        row.addView(titleText, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(new Space(this), new LinearLayout.LayoutParams(dp(56), dp(56)));
        return row;
    }

    private LinearLayout itemCard(int icon, String title, String subtitle, String extra, boolean locked, Runnable tap, Runnable lockAction, Runnable menuAction) {
        LinearLayout box = card();
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout row = hrow();
        row.addView(frameIcon(icon, RED, dp(44)), new LinearLayout.LayoutParams(dp(48), dp(58)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title, 18, INK, true));
        if (!subtitle.isEmpty()) text.addView(label(subtitle, 14, MUTED, false));
        if (!extra.isEmpty()) text.addView(label(extra, 14, RED, true));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(8), 0, dp(6), 0);
        row.addView(text, textParams);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        ImageButton lock = plainIconButton(locked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open, locked ? RED_DARK : GOLD, dp(3));
        lock.setContentDescription(locked ? "Protegido" : "Desprotegido");
        lock.setOnClickListener(v -> {
            v.setEnabled(false);
            lockAction.run();
        });
        actions.addView(lock, new LinearLayout.LayoutParams(dp(42), dp(32)));
        ImageButton menu = moreMenuButton(RED);
        menu.setContentDescription("Opcoes");
        menu.setOnClickListener(v -> menuAction.run());
        actions.addView(menu, new LinearLayout.LayoutParams(dp(42), dp(38)));
        row.addView(actions, new LinearLayout.LayoutParams(dp(46), dp(72)));
        box.addView(row);
        if (tap != null) box.setOnClickListener(v -> tap.run());
        return box;
    }

    private void addMenuButton(LinearLayout card, Runnable action) {
        LinearLayout row = (LinearLayout) card.getChildAt(0);
        ImageButton menu = moreMenuButton(RED);
        menu.setOnClickListener(v -> action.run());
        row.addView(menu, new LinearLayout.LayoutParams(dp(42), dp(48)));
    }

    private LinearLayout empty(String title, String subtitle) {
        LinearLayout box = card();
        box.addView(titleRow(currentFrameIcon(), title, 20));
        box.addView(label(subtitle, 15, MUTED, false));
        return box;
    }

    private int currentFrameIcon() {
        if ("caderno".equals(screen)) return R.drawable.ic_category;
        if ("categoria".equals(screen)) return R.drawable.ic_recipe;
        if ("receita".equals(screen)) return R.drawable.ic_ingredient;
        if ("ingredientes_caderno".equals(screen)) return R.drawable.ic_clipboard_list;
        return R.drawable.ic_book;
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setStroke(dp(1), LINE);
        bg.setCornerRadius(dp(18));
        box.setBackground(bg);
        box.setElevation(dp(3));
        return box;
    }

    private LinearLayout logoCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PAPER);
        bg.setStroke(dp(1), Color.argb(90, 232, 201, 142));
        bg.setCornerRadius(dp(22));
        box.setBackground(bg);
        box.setElevation(dp(4));
        return box;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text == null ? "" : text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(true);
        v.setSingleLine(false);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout titleRow(int icon, String title, int sp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(6));
        row.addView(frameIcon(icon, RED, dp(34)), new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView text = label(title, sp, RED, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(8), 0, 0, 0);
        row.addView(text, lp);
        return row;
    }

    private FrameLayout frameIcon(int drawable, int color, int size) {
        FrameLayout holder = new FrameLayout(this);
        holder.setBackground(round(Color.argb(44, 217, 154, 59), dp(13), Color.argb(120, 232, 201, 142), 1));
        ImageView icon = new ImageView(this);
        icon.setImageResource(drawable);
        icon.setColorFilter(color);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
        holder.addView(icon, params);
        return holder;
    }

    private LinearLayout themedDialogBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), 0);
        return box;
    }

    private AlertDialog.Builder themedDialog(String title, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (title != null && !title.isEmpty()) {
            TextView titleView = label(title, 20, RED, true);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(dp(18), dp(18), dp(18), dp(2));
            builder.setCustomTitle(titleView);
        }
        if (view != null) builder.setView(view);
        return builder;
    }

    private AlertDialog showThemed(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> styleDialogWindow(dialog));
        dialog.show();
        styleDialogWindow(dialog);
        return dialog;
    }

    private EditText entry(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(false);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setSelectAllOnFocus(true);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(fieldBg());
        e.setLayoutParams(fieldParams());
        return e;
    }

    private AutoCompleteTextView autoEntry(String hint, List<String> values) {
        AutoCompleteTextView e = new AutoCompleteTextView(this);
        e.setHint(hint);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setThreshold(1);
        e.setSelectAllOnFocus(true);
        e.setAdapter(textAdapter(values));
        e.setDropDownBackgroundDrawable(round(PAPER, dp(14), LINE, 1));
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(fieldBg());
        e.setLayoutParams(fieldParams());
        return e;
    }

    private MultiAutoCompleteTextView prepEntry(String hint, String value, List<String> values) {
        MultiAutoCompleteTextView e = new MultiAutoCompleteTextView(this);
        e.setHint(hint);
        e.setText(value);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setThreshold(1);
        e.setMinLines(7);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setAdapter(textAdapter(values));
        e.setTokenizer(new IngredientTokenizer());
        e.setDropDownBackgroundDrawable(round(PAPER, dp(14), LINE, 1));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackground(fieldBg());
        e.setLayoutParams(fieldParams());
        return e;
    }

    private ArrayAdapter<String> textAdapter(List<String> values) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleAdapterText(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                styleAdapterText(view);
                view.setBackgroundColor(PAPER);
                return view;
            }
        };
    }

    private void styleAdapterText(TextView view) {
        view.setTextColor(INK);
        view.setHintTextColor(MUTED);
        view.setTextSize(16);
        view.setPadding(dp(14), dp(10), dp(14), dp(10));
    }

    private GradientDrawable fieldBg() {
        return round(CARD_STRONG, dp(14), LINE, 1);
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    private void addActionButton(LinearLayout parent, int drawable, String desc, View.OnClickListener listener) {
        LinearLayout strip = iconStrip();
        addWeightedStripIcon(strip, drawable, RED, desc, listener);
        parent.addView(strip, actionStripParams());
    }

    private LinearLayout iconStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(dp(5), dp(5), dp(5), dp(5));
        strip.setBackground(round(CARD_STRONG, dp(18), LINE, 1));
        strip.setElevation(dp(4));
        return strip;
    }

    private ImageButton addWeightedStripIcon(LinearLayout strip, int drawable, int color, String desc, View.OnClickListener listener) {
        if (strip.getChildCount() > 0) addStripDivider(strip);
        ImageButton button = plainIconButton(drawable, color, dp(9));
        button.setContentDescription(desc);
        if (listener != null) button.setOnClickListener(listener);
        strip.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return button;
    }

    private void addStripDivider(LinearLayout strip) {
        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(115, 184, 50, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(dp(3), dp(10), dp(3), dp(10));
        strip.addView(divider, params);
    }

    private ImageButton plainIconButton(int drawable, int fg, int padding) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawable);
        button.setColorFilter(fg);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return button;
    }

    private ImageButton moreMenuButton(int fg) {
        ImageButton button = plainIconButton(R.drawable.ic_more_vertical, fg, dp(2));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return button;
    }

    private ImageButton imageIconButton(int drawable, int bg, int fg) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawable);
        button.setColorFilter(fg);
        button.setBackground(outlineButtonBg(bg, dp(16)));
        button.setPadding(dp(11), dp(11), dp(11), dp(11));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setElevation(dp(4));
        return button;
    }

    private LinearLayout.LayoutParams actionStripParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(64));
        params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private GradientDrawable outlineButtonBg(int color, int radius) {
        return round(color, radius, Color.argb(170, 255, 236, 196), 1);
    }

    private GradientDrawable round(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(dp(strokeWidth), stroke);
        return bg;
    }

    private LinearLayout hrow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, 0);
        return row;
    }

    private void addSearch(String hint, Runnable callback) {
        search = searchEntry(hint);
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { callback.run(); }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(56));
        params.setMargins(0, 0, 0, dp(12));
        root.addView(search, params);
    }

    private EditText searchEntry(String hint) {
        EditText e = entry(hint, "");
        Drawable icon = getResources().getDrawable(R.drawable.ic_search, getTheme());
        icon.setTint(RED);
        e.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
        e.setCompoundDrawablePadding(dp(10));
        e.setBackground(round(CARD_STRONG, dp(18), LINE, 1));
        return e;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1);
        lp.setMargins(dp(4), dp(8), dp(4), 0);
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams matchWrapWithTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, top, 0, 0);
        return lp;
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(RED_DARK);
        window.setNavigationBarColor(PAPER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private View statusBarShield() {
        View bar = new View(this);
        bar.setBackgroundColor(RED_DARK);
        bar.setElevation(dp(8));
        return bar;
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private void actions(String title, String[] items, Choice choice) {
        showThemed(themedDialog(title, null)
            .setItems(items, (d, which) -> choice.pick(which))
        );
    }

    private void editOne(String title, String h1, String v1, SaveOne save) {
        LinearLayout box = themedDialogBox();
        EditText a = entry(h1, v1);
        box.addView(a);
        showThemed(themedDialog(title, box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(a)) return;
                save.save(text(a));
            }));
    }

    private void editTwo(String title, String h1, String v1, String h2, String v2, SaveTwo save) {
        LinearLayout box = themedDialogBox();
        EditText a = entry(h1, v1);
        EditText b = entry(h2, v2);
        box.addView(a);
        box.addView(b);
        showThemed(themedDialog(title, box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", (d, w) -> {
                if (blank(a)) return;
                save.save(text(a), text(b));
            }));
    }

    private void confirm(String title, String message, Runnable yes) {
        showThemed(themedDialog(title, null)
            .setMessage(message)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("OK", (d, w) -> yes.run())
        );
    }

    private void confirmDelete(String table, Item item, String label, String message, Runnable deleteAction) {
        if (db.isLocked(table, item.id)) {
            showThemed(themedDialog("Item protegido", null)
                .setMessage("O " + label + " \"" + item.name + "\" esta com o cadeado fechado. Abra o cadeado antes de excluir.")
                .setPositiveButton("Entendi", null));
            return;
        }
        showThemed(themedDialog("Confirmar exclusao", null)
            .setMessage(message + "\n\nDeseja mesmo excluir \"" + item.name + "\"? Esta acao nao pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir", (d, w) -> deleteAction.run()));
    }

    private void toggleLock(String table, Item item, Runnable refresh) {
        boolean next = !db.isLocked(table, item.id);
        db.setLocked(table, item.id, next);
        toast(next ? "Protecao ativada." : "Protecao removida.");
        refresh.run();
    }

    private void styleDialogWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PAPER);
        bg.setStroke(dp(1), LINE);
        bg.setCornerRadius(dp(18));
        window.setBackgroundDrawable(bg);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (positive != null) positive.setTextColor(RED);
        if (negative != null) negative.setTextColor(MUTED);
        if (neutral != null) neutral.setTextColor(GOLD);
        ListView list = dialog.getListView();
        if (list != null) {
            list.setBackgroundColor(PAPER);
            list.setDividerHeight(1);
            list.setDivider(round(Color.argb(70, 232, 201, 142), 0, Color.TRANSPARENT, 0));
        }
    }

    @Override
    public void onBackPressed() {
        if ("quiz".equals(screen) || "quiz_result".equals(screen) || "game_over".equals(screen)) showCaderno(currentCadernoId);
        else if ("recipe_preview".equals(screen)) showCategoria(currentCategoriaId);
        else if ("ingredientes_caderno".equals(screen)) showCaderno(currentCadernoId);
        else if ("receita".equals(screen)) backFromReceita();
        else if ("categoria".equals(screen)) showCaderno(currentCadernoId);
        else if ("caderno".equals(screen)) showHome();
        else super.onBackPressed();
    }

    private String readUrl(String value) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(value).openConnection();
        con.setConnectTimeout(12000);
        con.setReadTimeout(12000);
        try (InputStream in = con.getInputStream()) {
            return new String(readAll(in), "UTF-8");
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format(Locale.US, "%02X", b));
        return sb.toString();
    }

    private void ui(Runnable r) { runOnUiThread(r); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private boolean blank(TextView v) { return text(v).isEmpty(); }
    private String text(TextView v) { return v == null ? "" : v.getText().toString().trim(); }
    private String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT);
    }

    private ArrayList<String> ingredientNames(List<Item> ingredients) {
        ArrayList<String> names = new ArrayList<>();
        for (Item item : ingredients) addUnique(names, item.name);
        return names;
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (!containsNorm(values, value)) values.add(value.trim());
    }

    private ArrayList<String> withoutNormalized(List<String> source, List<String> excluded) {
        ArrayList<String> out = new ArrayList<>();
        for (String value : source) {
            if (!containsNorm(excluded, value)) addUnique(out, value);
        }
        return out;
    }

    private ArrayList<String> pickDistractors(List<String> source, String correct, int count) {
        ArrayList<String> pool = new ArrayList<>();
        for (String value : source) {
            if (!norm(value).equals(norm(correct))) addUnique(pool, value);
        }
        Collections.shuffle(pool);
        ArrayList<String> out = new ArrayList<>();
        for (String value : pool) {
            if (out.size() == count) break;
            out.add(value);
        }
        return out;
    }

    private boolean containsNorm(List<String> values, String value) {
        String wanted = norm(value);
        for (String existing : values) {
            if (norm(existing).equals(wanted)) return true;
        }
        return false;
    }

    private int indexOfNorm(List<String> values, String value) {
        String wanted = norm(value);
        for (int i = 0; i < values.size(); i++) {
            if (norm(values.get(i)).equals(wanted)) return i;
        }
        return 0;
    }

    private String prepSnippet(String value) {
        String clean = value == null ? "" : value.replace("\r", " ").replace("\n", " ").trim();
        while (clean.contains("  ")) clean = clean.replace("  ", " ");
        if (clean.length() > 110) return clean.substring(0, 107).trim() + "...";
        return clean;
    }

    private void openLinkedReceita(int recipeId) {
        backStack.push(new NavState(screen, currentCadernoId, currentCategoriaId, currentReceitaId));
        showReceita(recipeId);
    }

    private void backFromReceita() {
        if (!backStack.isEmpty()) {
            NavState state = backStack.pop();
            currentCadernoId = state.cadernoId;
            currentCategoriaId = state.categoriaId;
            currentReceitaId = state.receitaId;
            if ("receita".equals(state.screen)) showReceita(state.receitaId);
            else if ("categoria".equals(state.screen)) showCategoria(state.categoriaId);
            else if ("caderno".equals(state.screen)) showCaderno(state.cadernoId);
            else showHome();
            return;
        }
        showCategoria(currentCategoriaId);
    }

    private String categoryHintFor(String ingredientName) {
        String known = db.categoryForIngredient(ingredientName);
        if (!known.isEmpty()) return "Categoria sugerida: " + known;
        return "Categoria (ex: gordura, molho, tempero)";
    }

    interface Choice { void pick(int which); }
    interface SaveOne { void save(String a); }
    interface SaveTwo { void save(String a, String b); }

    abstract class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    static class IngredientTokenizer implements MultiAutoCompleteTextView.Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0) {
                char c = text.charAt(i - 1);
                if (c == ',' || c == '\n' || c == ';' || c == '.') break;
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') i++;
            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                char c = text.charAt(i);
                if (c == ',' || c == '\n' || c == ';' || c == '.') return i;
                i++;
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            return text;
        }
    }

    static class NavState {
        final String screen;
        final int cadernoId;
        final int categoriaId;
        final int receitaId;

        NavState(String screen, int cadernoId, int categoriaId, int receitaId) {
            this.screen = screen;
            this.cadernoId = cadernoId;
            this.categoriaId = categoriaId;
            this.receitaId = receitaId;
        }
    }

    static class QuizQuestion {
        final String prompt;
        final ArrayList<String> options;
        final int correctIndex;

        QuizQuestion(String prompt, ArrayList<String> options, int correctIndex) {
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    static class Item {
        int id;
        int parentId;
        int cadernoId;
        int recipeLinkId;
        boolean locked = true;
        String name = "";
        String desc = "";
        String extra = "";
    }

    class Db extends SQLiteOpenHelper {
        Db(Context context) { super(context, "caderno_receitas_java.db", null, 3); }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE cadernos(id INTEGER PRIMARY KEY AUTOINCREMENT,nome TEXT NOT NULL,descricao TEXT,criado INTEGER,bloqueado INTEGER NOT NULL DEFAULT 1)");
            db.execSQL("CREATE TABLE categorias(id INTEGER PRIMARY KEY AUTOINCREMENT,caderno_id INTEGER NOT NULL,nome TEXT NOT NULL,descricao TEXT,criado INTEGER,bloqueado INTEGER NOT NULL DEFAULT 1)");
            db.execSQL("CREATE TABLE receitas(id INTEGER PRIMARY KEY AUTOINCREMENT,caderno_id INTEGER NOT NULL,categoria_id INTEGER NOT NULL,nome TEXT NOT NULL,preparo TEXT,bloqueado INTEGER NOT NULL DEFAULT 1)");
            db.execSQL("CREATE TABLE ingredientes(id INTEGER PRIMARY KEY AUTOINCREMENT,receita_id INTEGER NOT NULL,nome TEXT NOT NULL,quantidade TEXT,categoria TEXT,receita_link_id INTEGER NOT NULL DEFAULT 0,bloqueado INTEGER NOT NULL DEFAULT 1)");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE ingredientes ADD COLUMN receita_link_id INTEGER NOT NULL DEFAULT 0");
            }
            if (oldVersion < 3) {
                addColumnIfMissing(db, "cadernos", "bloqueado", "INTEGER NOT NULL DEFAULT 1");
                addColumnIfMissing(db, "categorias", "bloqueado", "INTEGER NOT NULL DEFAULT 1");
                addColumnIfMissing(db, "receitas", "bloqueado", "INTEGER NOT NULL DEFAULT 1");
                addColumnIfMissing(db, "ingredientes", "bloqueado", "INTEGER NOT NULL DEFAULT 1");
            }
        }

        Item get(String table, int id) {
            try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + table + " WHERE id=?", new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) {
                    Item item = new Item();
                    item.id = id;
                    item.name = c.getString(c.getColumnIndexOrThrow("nome"));
                    item.desc = c.getString(c.getColumnIndexOrThrow("descricao"));
                    if (hasColumn(c, "caderno_id")) item.parentId = c.getInt(c.getColumnIndexOrThrow("caderno_id"));
                    if (hasColumn(c, "bloqueado")) item.locked = c.getInt(c.getColumnIndexOrThrow("bloqueado")) != 0;
                    return item;
                }
            }
            return new Item();
        }

        Item getReceita(int id) {
            try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM receitas WHERE id=?", new String[]{String.valueOf(id)})) {
                if (c.moveToFirst()) {
                    Item item = new Item();
                    item.id = id;
                    item.cadernoId = c.getInt(c.getColumnIndexOrThrow("caderno_id"));
                    item.parentId = c.getInt(c.getColumnIndexOrThrow("categoria_id"));
                    item.name = c.getString(c.getColumnIndexOrThrow("nome"));
                    item.desc = c.getString(c.getColumnIndexOrThrow("preparo"));
                    if (hasColumn(c, "bloqueado")) item.locked = c.getInt(c.getColumnIndexOrThrow("bloqueado")) != 0;
                    return item;
                }
            }
            return new Item();
        }

        List<Item> cadernos(String q) {
            return list("SELECT id,nome,descricao,'' extra,0 parent_id,0 caderno_id,0 receita_link_id,bloqueado FROM cadernos ORDER BY nome", q);
        }

        List<Item> categorias(int caderno, String q) {
            return list("SELECT id,nome,descricao,'' extra,caderno_id parent_id,caderno_id,0 receita_link_id,bloqueado FROM categorias WHERE caderno_id=" + caderno + " ORDER BY nome", q);
        }

        List<Item> receitas(int categoria, String q) {
            return list("SELECT id,nome,preparo descricao,'' extra,categoria_id parent_id,caderno_id,0 receita_link_id,bloqueado FROM receitas WHERE categoria_id=" + categoria + " ORDER BY nome", q);
        }

        List<Item> receitasCaderno(int caderno) {
            return list("SELECT id,nome,preparo descricao,'' extra,categoria_id parent_id,caderno_id,0 receita_link_id,bloqueado FROM receitas WHERE caderno_id=" + caderno + " ORDER BY nome", "");
        }

        List<Item> ingredientes(int receita, String q) {
            return list("SELECT id,nome,quantidade descricao,categoria extra,receita_id parent_id,0 caderno_id,receita_link_id,bloqueado FROM ingredientes WHERE receita_id=" + receita + " ORDER BY categoria,nome", q);
        }

        List<Item> list(String sql, String q) {
            ArrayList<Item> out = new ArrayList<>();
            String nq = norm(q);
            try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
                while (c.moveToNext()) {
                    Item item = new Item();
                    item.id = c.getInt(0);
                    item.name = safe(c.getString(1));
                    item.desc = safe(c.getString(2));
                    item.extra = safe(c.getString(3));
                    item.parentId = c.getInt(4);
                    item.cadernoId = c.getInt(5);
                    if (c.getColumnCount() > 6) item.recipeLinkId = c.getInt(6);
                    if (c.getColumnCount() > 7) item.locked = c.getInt(7) != 0;
                    if (nq.isEmpty() || norm(item.name + " " + item.desc + " " + item.extra).contains(nq)) out.add(item);
                }
            }
            return out;
        }

        void saveCaderno(int id, String nome, String desc) {
            ContentValues v = values(nome, desc);
            if (id == 0) {
                v.put("criado", System.currentTimeMillis());
                getWritableDatabase().insert("cadernos", null, v);
            } else getWritableDatabase().update("cadernos", v, "id=?", new String[]{String.valueOf(id)});
        }

        void saveCategoria(int id, int caderno, String nome, String desc) {
            ContentValues v = values(nome, desc);
            v.put("caderno_id", caderno);
            if (id == 0) {
                v.put("criado", System.currentTimeMillis());
                getWritableDatabase().insert("categorias", null, v);
            } else getWritableDatabase().update("categorias", v, "id=?", new String[]{String.valueOf(id)});
        }

        void saveReceita(int id, int caderno, int categoria, String nome, String preparo) {
            ContentValues v = new ContentValues();
            v.put("caderno_id", caderno);
            v.put("categoria_id", categoria);
            v.put("nome", nome);
            v.put("preparo", preparo);
            if (id == 0) getWritableDatabase().insert("receitas", null, v);
            else getWritableDatabase().update("receitas", v, "id=?", new String[]{String.valueOf(id)});
        }

        void saveIngrediente(int id, int receita, String nome, String qtd, String categoria, int recipeLinkId) {
            ContentValues v = new ContentValues();
            v.put("receita_id", receita);
            v.put("nome", nome);
            v.put("quantidade", qtd);
            v.put("categoria", categoria);
            v.put("receita_link_id", recipeLinkId);
            if (id == 0) getWritableDatabase().insert("ingredientes", null, v);
            else getWritableDatabase().update("ingredientes", v, "id=?", new String[]{String.valueOf(id)});
        }

        List<Item> receitasParaVincular(int receitaAtual) {
            return list("SELECT id,nome,preparo descricao,'' extra,categoria_id parent_id,caderno_id,0 receita_link_id,bloqueado FROM receitas WHERE id<>" + receitaAtual + " ORDER BY nome", "");
        }

        List<Item> ingredientesCaderno(int caderno, String q) {
            return list("SELECT i.id,i.nome,i.quantidade descricao,(CASE WHEN IFNULL(i.categoria,'')='' THEN r.nome ELSE i.categoria || ' - ' || r.nome END) extra,i.receita_id parent_id,r.caderno_id caderno_id,i.receita_link_id,i.bloqueado FROM ingredientes i JOIN receitas r ON r.id=i.receita_id WHERE r.caderno_id=" + caderno + " ORDER BY i.nome,r.nome", q);
        }

        int findRecipeByName(String name, int receitaAtual) {
            String wanted = norm(name);
            if (wanted.isEmpty()) return 0;
            for (Item receita : receitasParaVincular(receitaAtual)) {
                if (norm(receita.name).equals(wanted)) return receita.id;
            }
            return 0;
        }

        String categoryForIngredient(String ingredientName) {
            String wanted = norm(ingredientName);
            if (wanted.isEmpty()) return "";
            try (Cursor c = getReadableDatabase().rawQuery("SELECT nome,categoria FROM ingredientes WHERE categoria<>'' ORDER BY id DESC", null)) {
                while (c.moveToNext()) {
                    if (norm(c.getString(0)).equals(wanted)) return safe(c.getString(1));
                }
            }
            if (findRecipeByName(ingredientName, currentReceitaId) > 0) return "Receita preparada";
            return "";
        }

        List<String> ingredientNamesForReceita(int receitaId) {
            return names("SELECT DISTINCT nome FROM ingredientes WHERE receita_id=" + receitaId + " ORDER BY nome");
        }

        List<String> ingredientAndRecipeNames(int receitaAtual) {
            ArrayList<String> out = new ArrayList<>();
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            unique.addAll(ingredientNames());
            for (Item receita : receitasParaVincular(receitaAtual)) unique.add(receita.name);
            out.addAll(unique);
            return out;
        }

        void delete(String table, int id) {
            getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)});
        }

        boolean isLocked(String table, int id) {
            try (Cursor c = getReadableDatabase().rawQuery("SELECT bloqueado FROM " + table + " WHERE id=?", new String[]{String.valueOf(id)})) {
                return !c.moveToFirst() || c.getInt(0) != 0;
            }
        }

        void setLocked(String table, int id, boolean locked) {
            ContentValues v = new ContentValues();
            v.put("bloqueado", locked ? 1 : 0);
            getWritableDatabase().update(table, v, "id=?", new String[]{String.valueOf(id)});
        }

        void deleteCaderno(int id) {
            for (Item c : categorias(id, "")) deleteCategoria(c.id);
            delete("cadernos", id);
        }

        void deleteCategoria(int id) {
            for (Item r : receitas(id, "")) deleteReceita(r.id);
            delete("categorias", id);
        }

        void deleteReceita(int id) {
            getWritableDatabase().delete("ingredientes", "receita_id=?", new String[]{String.valueOf(id)});
            delete("receitas", id);
        }

        int countReceitasCaderno(int id) { return count("SELECT COUNT(*) FROM receitas WHERE caderno_id=" + id); }
        int countReceitasCategoria(int id) { return count("SELECT COUNT(*) FROM receitas WHERE categoria_id=" + id); }
        int countIngredientes(int id) { return count("SELECT COUNT(*) FROM ingredientes WHERE receita_id=" + id); }

        int count(String sql) {
            try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
                return c.moveToFirst() ? c.getInt(0) : 0;
            }
        }

        List<String> cadernoNames() { return names("SELECT DISTINCT nome FROM cadernos ORDER BY nome"); }
        List<String> ingredientNames() { return names("SELECT DISTINCT nome FROM ingredientes ORDER BY nome"); }
        List<String> ingredientCategories() { return names("SELECT DISTINCT categoria FROM ingredientes WHERE categoria<>'' ORDER BY categoria"); }

        List<String> names(String sql) {
            ArrayList<String> out = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
                while (c.moveToNext()) out.add(safe(c.getString(0)));
            }
            return out;
        }

        ContentValues values(String nome, String desc) {
            ContentValues v = new ContentValues();
            v.put("nome", nome);
            v.put("descricao", desc);
            return v;
        }

        boolean hasColumn(Cursor c, String name) {
            return c.getColumnIndex(name) >= 0;
        }

        void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
            try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
                while (c.moveToNext()) {
                    if (column.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) return;
                }
            }
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }

        String safe(String s) { return s == null ? "" : s; }
    }
}
