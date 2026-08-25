package com.encarvoicesearch;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/*
 * ============================================================
 * ENCAR AUTO CATALOG SCANNER
 *
 * Сканира автоматично:
 *
 * KIA
 * HYUNDAI
 * MERCEDES
 * BMW
 * AUDI
 *
 * Нива:
 *
 * Manufacturer
 * ModelGroup
 * Model
 * Grade
 * BadgeGroup
 * Badge
 * FuelType
 *
 * Всеки HTTP response се записва RAW във файл.
 * ============================================================
 */

public class MainActivity extends Activity {

    private static final String API =
            "https://api.encar.com/search/car/list/general";

    private static final String ENCAR_HOME =
            "https://car.encar.com/list/car";


    /*
     * Не искам скенерът да удря Encar прекалено агресивно.
     *
     * 450 ms между заявките.
     */
    private static final long REQUEST_DELAY_MS =
            450L;


    /*
     * Защита от безкраен цикъл.
     */
    private static final int MAX_REQUESTS =
            3000;


    private TextView status;
    private TextView outputView;

    private Button startButton;
    private Button stopButton;

    private WebView cookieWebView;


    private volatile boolean stopRequested =
            false;

    private volatile boolean scanning =
            false;

    private volatile boolean cookieReady =
            false;

    private volatile boolean startPending =
            false;


    private String userAgent =
            "Mozilla/5.0";


    private BufferedWriter fileWriter;

    private Uri downloadUri;

    private String outputFileName =
            "";

    private String fallbackFilePath =
            "";


    /*
     * Само структурираното обобщение държим в RAM.
     *
     * Огромните RAW JSON отговори отиват директно във файла.
     */
    private final StringBuilder summary =
            new StringBuilder();


    private final List<Brand> brands =
            new ArrayList<>();


    // =========================================================
    // DATA CLASSES
    // =========================================================

    private static class Brand {

        String display;
        String manufacturer;
        String carType;

        Brand(
                String display,
                String manufacturer,
                String carType
        ) {

            this.display =
                    display;

            this.manufacturer =
                    manufacturer;

            this.carType =
                    carType;
        }
    }


    private static class Task {

        Brand brand;

        String modelGroup;
        String model;

        String grade;
        String badgeGroup;

        int depth;


        Task(
                Brand brand
        ) {

            this.brand =
                    brand;

            this.depth =
                    0;
        }


        Task copy() {

            Task t =
                    new Task(
                            brand
                    );

            t.modelGroup =
                    modelGroup;

            t.model =
                    model;

            t.grade =
                    grade;

            t.badgeGroup =
                    badgeGroup;

            t.depth =
                    depth;

            return t;
        }
    }


    private static class ApiResult {

        int code;

        String url;
        String body;

        ApiResult(
                int code,
                String url,
                String body
        ) {

            this.code =
                    code;

            this.url =
                    url;

            this.body =
                    body;
        }
    }


    private static class Found {

        Set<String> modelGroups =
                new LinkedHashSet<>();

        Set<String> models =
                new LinkedHashSet<>();

        Set<String> grades =
                new LinkedHashSet<>();

        Set<String> badgeGroups =
                new LinkedHashSet<>();

        Set<String> badges =
                new LinkedHashSet<>();

        Set<String> fuels =
                new LinkedHashSet<>();
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        initBrands();

        buildUi();

        initCookieWebView();
    }


    // =========================================================
    // BRANDS
    // =========================================================

    private void initBrands() {

        brands.clear();


        /*
         * Korean/local branch
         */
        brands.add(
                new Brand(
                        "KIA",
                        "기아",
                        "Y"
                )
        );


        brands.add(
                new Brand(
                        "HYUNDAI",
                        "현대",
                        "Y"
                )
        );


        /*
         * Imported branch
         */
        brands.add(
                new Brand(
                        "MERCEDES",
                        "벤츠",
                        "N"
                )
        );


        brands.add(
                new Brand(
                        "BMW",
                        "BMW",
                        "N"
                )
        );


        brands.add(
                new Brand(
                        "AUDI",
                        "아우디",
                        "N"
                )
        );
    }


    // =========================================================
    // UI
    // =========================================================

    private void buildUi() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );


        TextView title =
                new TextView(this);

        title.setText(
                "ENCAR AUTO CATALOG SCANNER"
        );

        title.setTextSize(
                20
        );

        title.setPadding(
                20,
                20,
                20,
                15
        );


        status =
                new TextView(this);

        status.setText(
                "Подготвям Encar..."
        );

        status.setTextSize(
                15
        );

        status.setPadding(
                20,
                10,
                20,
                10
        );

        status.setTextIsSelectable(
                true
        );


        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );


        startButton =
                new Button(this);

        startButton.setText(
                "▶ SCAN ALL"
        );


        stopButton =
                new Button(this);

        stopButton.setText(
                "■ STOP"
        );


        Button copyButton =
                new Button(this);

        copyButton.setText(
                "КОПИРАЙ"
        );


        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );


        buttons.addView(
                startButton,
                bp
        );

        buttons.addView(
                stopButton,
                bp
        );

        buttons.addView(
                copyButton,
                bp
        );


        outputView =
                new TextView(this);

        outputView.setTextSize(
                13
        );

        outputView.setTextIsSelectable(
                true
        );

        outputView.setPadding(
                20,
                15,
                20,
                30
        );


        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                outputView
        );


        cookieWebView =
                new WebView(this);

        cookieWebView.setVisibility(
                View.INVISIBLE
        );


        root.addView(
                title
        );

        root.addView(
                status
        );

        root.addView(
                buttons
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );


        root.addView(
                cookieWebView,
                new LinearLayout.LayoutParams(
                        1,
                        1
                )
        );


        setContentView(
                root
        );


        startButton.setOnClickListener(
                v -> startAll()
        );


        stopButton.setOnClickListener(
                v -> {

                    stopRequested =
                            true;

                    status.setText(
                            "STOP поискан. Завършвам текущата заявка..."
                    );
                }
        );


        copyButton.setOnClickListener(
                v -> copySummary()
        );
    }


    // =========================================================
    // COOKIE WEBVIEW
    // =========================================================

    private void initCookieWebView() {

        WebSettings settings =
                cookieWebView.getSettings();


        settings.setJavaScriptEnabled(
                true
        );

        settings.setDomStorageEnabled(
                true
        );

        settings.setDatabaseEnabled(
                true
        );


        userAgent =
                settings.getUserAgentString();


        CookieManager manager =
                CookieManager.getInstance();


        manager.setAcceptCookie(
                true
        );


        manager.setAcceptThirdPartyCookies(
                cookieWebView,
                true
        );


        cookieWebView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );


                        CookieManager
                                .getInstance()
                                .flush();


                        cookieReady =
                                true;


                        status.setText(
                                "Encar е готов.\n" +
                                "Натисни SCAN ALL."
                        );


                        if (
                                startPending
                        ) {

                            startPending =
                                    false;

                            startAll();
                        }
                    }
                }
        );


        cookieWebView.loadUrl(
                ENCAR_HOME
        );
    }


    // =========================================================
    // START
    // =========================================================

    private void startAll() {

        if (
                scanning
        ) {

            Toast.makeText(
                    this,
                    "Скенерът вече работи.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (
                !cookieReady
        ) {

            startPending =
                    true;

            status.setText(
                    "Изчаквам Encar да се зареди..."
            );

            cookieWebView.loadUrl(
                    ENCAR_HOME
            );

            return;
        }


        stopRequested =
                false;

        scanning =
                true;


        summary.setLength(
                0
        );


        outputView.setText(
                ""
        );


        startButton.setEnabled(
                false
        );


        new Thread(
                this::runScan
        ).start();
    }


    // =========================================================
    // MAIN SCAN
    // =========================================================

    private void runScan() {

        ArrayDeque<Task> queue =
                new ArrayDeque<>();


        Set<String> seenQueries =
                new LinkedHashSet<>();


        int requestCount =
                0;


        try {

            openOutputFile();


            writeHeader();


            /*
             * Първа задача:
             * една заявка за всяка марка.
             */
            for (
                    Brand brand :
                    brands
            ) {

                queue.add(
                        new Task(
                                brand
                        )
                );
            }


            while (
                    !queue.isEmpty()
                            &&
                    !stopRequested
                            &&
                    requestCount
                            <
                    MAX_REQUESTS
            ) {

                Task task =
                        queue.removeFirst();


                String q =
                        buildQ(
                                task
                        );


                if (
                        seenQueries.contains(
                                q
                        )
                ) {

                    continue;
                }


                seenQueries.add(
                        q
                );


                requestCount++;


                final int currentRequest =
                        requestCount;


                showProgress(
                        "Заявка "
                                +
                        currentRequest
                                +
                        "\n"
                                +
                        taskDescription(
                                task
                        )
                                +
                        "\n"
                                +
                        "Опашка: "
                                +
                        queue.size()
                );


                ApiResult response =
                        request(
                                q
                        );


                writeRawResponse(
                        currentRequest,
                        task,
                        q,
                        response
                );


                if (
                        response.code
                                ==
                        200
                ) {

                    Found found =
                            extractCatalog(
                                    response.body
                            );


                    writeFound(
                            task,
                            found
                    );


                    /*
                     * =================================================
                     * LEVEL 0
                     * BRAND → MODEL GROUP
                     * =================================================
                     */
                    if (
                            task.modelGroup
                                    ==
                            null
                                    &&
                            task.model
                                    ==
                            null
                    ) {

                        for (
                                String group :
                                found.modelGroups
                        ) {

                            Task child =
                                    task.copy();


                            child.modelGroup =
                                    group;

                            child.depth =
                                    1;


                            queue.addLast(
                                    child
                            );
                        }


                        /*
                         * Някои модели може да бъдат върнати
                         * директно без ModelGroup.
                         */
                        if (
                                found.modelGroups.isEmpty()
                        ) {

                            for (
                                    String model :
                                    found.models
                            ) {

                                Task child =
                                        task.copy();


                                child.model =
                                        model;

                                child.depth =
                                        2;


                                queue.addLast(
                                        child
                                );
                            }
                        }
                    }


                    /*
                     * =================================================
                     * MODEL GROUP → MODEL
                     * =================================================
                     */
                    else if (
                            task.modelGroup
                                    !=
                            null
                                    &&
                            task.model
                                    ==
                            null
                    ) {

                        for (
                                String model :
                                found.models
                        ) {

                            Task child =
                                    task.copy();


                            child.model =
                                    model;

                            child.depth =
                                    2;


                            queue.addLast(
                                    child
                            );
                        }
                    }


                    /*
                     * =================================================
                     * MODEL → GRADE / BADGE GROUP
                     * =================================================
                     */
                    else if (
                            task.model
                                    !=
                            null
                                    &&
                            task.grade
                                    ==
                            null
                                    &&
                            task.badgeGroup
                                    ==
                            null
                    ) {

                        for (
                                String grade :
                                found.grades
                        ) {

                            Task child =
                                    task.copy();


                            child.grade =
                                    grade;

                            child.depth =
                                    3;


                            queue.addLast(
                                    child
                            );
                        }


                        for (
                                String badgeGroup :
                                found.badgeGroups
                        ) {

                            Task child =
                                    task.copy();


                            child.badgeGroup =
                                    badgeGroup;

                            child.depth =
                                    3;


                            queue.addLast(
                                    child
                            );
                        }
                    }


                    /*
                     * GRADE / BadgeGroup response също се записва RAW.
                     *
                     * Не продължаваме безкрайно по Badge,
                     * защото самият response съдържа Badge стойностите.
                     */
                }


                else {

                    appendSummary(
                            "HTTP_ERROR | "
                                    +
                            taskDescription(
                                    task
                            )
                                    +
                            " | HTTP "
                                    +
                            response.code
                    );


                    /*
                     * Ако Encar спре достъпа,
                     * не се опитваме да заобикаляме защитата.
                     */
                    if (
                            response.code
                                    ==
                            403
                                    ||
                            response.code
                                    ==
                            407
                                    ||
                            response.code
                                    ==
                            429
                    ) {

                        stopRequested =
                                true;


                        appendSummary(
                                "SCAN_STOPPED_BY_HTTP_"
                                        +
                                response.code
                        );


                        break;
                    }
                }


                try {

                    Thread.sleep(
                            REQUEST_DELAY_MS
                    );

                } catch (
                        InterruptedException ignored
                ) {

                }
            }


            writeLine(
                    "\n\n===== SCAN FINISHED =====\n"
            );


            writeLine(
                    "REQUESTS="
                            +
                    requestCount
                            +
                    "\n"
            );


            writeLine(
                    "STOP_REQUESTED="
                            +
                    stopRequested
                            +
                    "\n"
            );


            writeLine(
                    "=========================\n"
            );


            closeOutputFile();


            final int finalRequestCount =
                    requestCount;


            runOnUiThread(
                    () -> {

                        scanning =
                                false;


                        startButton.setEnabled(
                                true
                        );


                        status.setText(
                                "СКАНЪТ ЗАВЪРШИ ✅\n"
                                        +
                                "Заявки: "
                                        +
                                finalRequestCount
                                        +
                                "\n"
                                        +
                                getSavedLocation()
                        );


                        outputView.setText(
                                summary.toString()
                        );
                    }
            );


        } catch (
                Exception e
        ) {

            try {

                writeLine(
                        "\nFATAL_ERROR="
                                +
                        e.toString()
                                +
                        "\n"
                );

            } catch (
                    Exception ignored
            ) {

            }


            closeOutputFile();


            runOnUiThread(
                    () -> {

                        scanning =
                                false;


                        startButton.setEnabled(
                                true
                        );


                        status.setText(
                                "ГРЕШКА:\n"
                                        +
                                e.getClass()
                                        .getSimpleName()
                                        +
                                "\n"
                                        +
                                e.getMessage()
                                        +
                                "\n\n"
                                        +
                                getSavedLocation()
                        );
                    }
            );
        }
    }


    // =========================================================
    // BUILD Q
    // =========================================================

    private String buildQ(
            Task task
    ) {

        StringBuilder q =
                new StringBuilder();


        q.append(
                "(And.Hidden.N."
        );


        q.append(
                "_.CarType."
        );


        q.append(
                task.brand.carType
        );


        q.append(
                "."
        );


        q.append(
                "_.Manufacturer."
        );


        q.append(
                task.brand.manufacturer
        );


        q.append(
                "."
        );


        if (
                task.modelGroup
                        !=
                null
        ) {

            q.append(
                    "_.ModelGroup."
            );


            q.append(
                    task.modelGroup
            );


            q.append(
                    "."
            );
        }


        if (
                task.model
                        !=
                null
        ) {

            q.append(
                    "_.Model."
            );


            q.append(
                    task.model
            );


            q.append(
                    "."
            );
        }


        if (
                task.grade
                        !=
                null
        ) {

            q.append(
                    "_.Grade."
            );


            q.append(
                    task.grade
            );


            q.append(
                    "."
            );
        }


        if (
                task.badgeGroup
                        !=
                null
        ) {

            q.append(
                    "_.BadgeGroup."
            );


            q.append(
                    task.badgeGroup
            );


            q.append(
                    "."
            );
        }


        q.append(
                ")"
        );


        return q.toString();
    }


    // =========================================================
    // HTTP REQUEST
    // =========================================================

    private ApiResult request(
            String q
    ) {

        HttpURLConnection connection =
                null;


        try {

            String url =
                    API
                            +
                    "?count=true"
                            +
                    "&q="
                            +
                    encode(
                            q
                    )
                            +
                    "&sr="
                            +
                    encode(
                            "|ModifiedDate|0|1"
                    )
                            +
                    "&inav="
                            +
                    encode(
                            "|Metadata|Sort"
                    );


            connection =
                    (HttpURLConnection)
                            new URL(
                                    url
                            )
                                    .openConnection();


            connection.setRequestMethod(
                    "GET"
            );


            connection.setConnectTimeout(
                    20000
            );


            connection.setReadTimeout(
                    30000
            );


            connection.setInstanceFollowRedirects(
                    true
            );


            connection.setRequestProperty(
                    "Accept",
                    "application/json, text/plain, */*"
            );


            connection.setRequestProperty(
                    "Accept-Language",
                    "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7"
            );


            connection.setRequestProperty(
                    "User-Agent",
                    userAgent == null
                            ?
                    "Mozilla/5.0"
                            :
                    userAgent
            );


            connection.setRequestProperty(
                    "Referer",
                    ENCAR_HOME
            );


            String cookies =
                    collectCookies();


            if (
                    !cookies.isEmpty()
            ) {

                connection.setRequestProperty(
                        "Cookie",
                        cookies
                );
            }


            int code =
                    connection.getResponseCode();


            InputStream stream =
                    code >= 200
                            &&
                    code < 400
                            ?
                    connection.getInputStream()
                            :
                    connection.getErrorStream();


            String body =
                    readAll(
                            stream
                    );


            return new ApiResult(
                    code,
                    url,
                    body
            );


        } catch (
                Exception e
        ) {

            return new ApiResult(
                    -1,
                    "",
                    e.getClass()
                            .getSimpleName()
                            +
                    ": "
                            +
                    (
                            e.getMessage()
                                    ==
                            null
                                    ?
                            ""
                                    :
                            e.getMessage()
                    )
            );


        } finally {

            if (
                    connection
                            !=
                    null
            ) {

                connection.disconnect();
            }
        }
    }


    // =========================================================
    // COOKIES
    // =========================================================

    private String collectCookies() {

        CookieManager manager =
                CookieManager.getInstance();


        Map<String, String> result =
                new LinkedHashMap<>();


        addCookies(
                result,
                manager.getCookie(
                        "https://car.encar.com"
                )
        );


        addCookies(
                result,
                manager.getCookie(
                        "https://m.encar.com"
                )
        );


        addCookies(
                result,
                manager.getCookie(
                        "https://api.encar.com"
                )
        );


        StringBuilder output =
                new StringBuilder();


        for (
                Map.Entry<String, String> entry :
                result.entrySet()
        ) {

            if (
                    output.length()
                            >
                    0
            ) {

                output.append(
                        "; "
                );
            }


            output.append(
                    entry.getKey()
            );


            output.append(
                    "="
            );


            output.append(
                    entry.getValue()
            );
        }


        return output.toString();
    }


    private void addCookies(
            Map<String, String> map,
            String cookieText
    ) {

        if (
                cookieText
                        ==
                null
                        ||
                cookieText.trim()
                        .isEmpty()
        ) {

            return;
        }


        String[] parts =
                cookieText.split(
                        ";"
                );


        for (
                String part :
                parts
        ) {

            String p =
                    part.trim();


            int index =
                    p.indexOf(
                            '='
                    );


            if (
                    index
                            >
                    0
            ) {

                map.put(
                        p.substring(
                                0,
                                index
                        ),
                        p.substring(
                                index + 1
                        )
                );
            }
        }
    }


    // =========================================================
    // EXTRACT CATALOG
    // =========================================================

    private Found extractCatalog(
            String body
    ) {

        Found found =
                new Found();


        if (
                body
                        ==
                null
                        ||
                body.isEmpty()
        ) {

            return found;
        }


        /*
         * 1.
         * Сканираме всички RAW strings за Encar filter tokens.
         */
        scanString(
                body,
                found
        );


        /*
         * 2.
         * Ако е JSON, обхождаме цялото дърво.
         */
        try {

            String trimmed =
                    body.trim();


            if (
                    trimmed.startsWith(
                            "{"
                    )
            ) {

                walkJson(
                        new JSONObject(
                                trimmed
                        ),
                        null,
                        found
                );

            } else if (
                    trimmed.startsWith(
                            "["
                    )
            ) {

                walkJson(
                        new JSONArray(
                                trimmed
                        ),
                        null,
                        found
                );
            }

        } catch (
                Exception ignored
        ) {

        }


        cleanupSet(
                found.modelGroups
        );

        cleanupSet(
                found.models
        );

        cleanupSet(
                found.grades
        );

        cleanupSet(
                found.badgeGroups
        );

        cleanupSet(
                found.badges
        );

        cleanupSet(
                found.fuels
        );


        return found;
    }


    // =========================================================
    // JSON WALKER
    // =========================================================

    private void walkJson(
            Object node,
            String context,
            Found found
    ) {

        try {

            if (
                    node
                            instanceof
                    JSONObject
            ) {

                JSONObject object =
                        (JSONObject)
                                node;


                JSONArray names =
                        object.names();


                if (
                        names
                                ==
                        null
                ) {

                    return;
                }


                /*
                 * Проверяваме дали object съдържа
                 * поле със стойност например "ModelGroup".
                 */
                String declaredType =
                        detectDeclaredType(
                                object
                        );


                for (
                        int i = 0;
                        i < names.length();
                        i++
                ) {

                    String key =
                            names.optString(
                                    i
                            );


                    Object value =
                            object.opt(
                                    key
                            );


                    String newContext =
                            context;


                    if (
                            isCatalogType(
                                    key
                            )
                    ) {

                        newContext =
                                normalizeType(
                                        key
                                );
                    }


                    if (
                            value
                                    instanceof
                            String
                    ) {

                        String text =
                                (String)
                                        value;


                        scanString(
                                text,
                                found
                        );


                        /*
                         * Ако key е директно:
                         *
                         * ModelGroup : "쏘렌토"
                         */
                        if (
                                isCatalogType(
                                        key
                                )
                        ) {

                            addByType(
                                    normalizeType(
                                            key
                                    ),
                                    text,
                                    found
                            );
                        }


                        /*
                         * Ако сме вътре в ModelGroup block
                         * и имаме Name/Value/Text/Label.
                         */
                        if (
                                newContext
                                        !=
                                null
                                        &&
                                isNameField(
                                        key
                                )
                        ) {

                            addByType(
                                    newContext,
                                    text,
                                    found
                            );
                        }


                        /*
                         * Например:
                         *
                         * Type = Model
                         * Value = 더 뉴 쏘렌토 4세대
                         */
                        if (
                                declaredType
                                        !=
                                null
                                        &&
                                isNameField(
                                        key
                                )
                        ) {

                            addByType(
                                    declaredType,
                                    text,
                                    found
                            );
                        }


                    } else if (
                            value
                                    instanceof
                            JSONObject
                                    ||
                            value
                                    instanceof
                            JSONArray
                    ) {

                        walkJson(
                                value,
                                newContext,
                                found
                        );
                    }
                }


            } else if (
                    node
                            instanceof
                    JSONArray
            ) {

                JSONArray array =
                        (JSONArray)
                                node;


                for (
                        int i = 0;
                        i < array.length();
                        i++
                ) {

                    Object child =
                            array.opt(
                                    i
                            );


                    if (
                            child
                                    instanceof
                            JSONObject
                                    ||
                            child
                                    instanceof
                            JSONArray
                    ) {

                        walkJson(
                                child,
                                context,
                                found
                        );


                    } else if (
                            child
                                    instanceof
                            String
                    ) {

                        String text =
                                (String)
                                        child;


                        scanString(
                                text,
                                found
                        );


                        if (
                                context
                                        !=
                                null
                        ) {

                            addByType(
                                    context,
                                    text,
                                    found
                            );
                        }
                    }
                }
            }

        } catch (
                Exception ignored
        ) {

        }
    }


    // =========================================================
    // DECLARED TYPE
    // =========================================================

    private String detectDeclaredType(
            JSONObject object
    ) {

        JSONArray names =
                object.names();


        if (
                names
                        ==
                null
        ) {

            return null;
        }


        for (
                int i = 0;
                i < names.length();
                i++
        ) {

            String key =
                    names.optString(
                            i
                    );


            Object value =
                    object.opt(
                            key
                    );


            if (
                    value
                            instanceof
                    String
            ) {

                String text =
                        (String)
                                value;


                if (
                        isCatalogType(
                                text
                        )
                ) {

                    return normalizeType(
                            text
                    );
                }
            }
        }


        return null;
    }


    // =========================================================
    // RAW STRING SCANNER
    // =========================================================

    private void scanString(
            String raw,
            Found found
    ) {

        if (
                raw
                        ==
                null
                        ||
                raw.isEmpty()
        ) {

            return;
        }


        String text =
                raw;


        /*
         * URL encoded strings също ги декодираме.
         */
        if (
                text.contains(
                        "%"
                )
        ) {

            try {

                String decoded =
                        URLDecoder.decode(
                                text,
                                "UTF-8"
                        );


                if (
                        !decoded.equals(
                                text
                        )
                ) {

                    scanToken(
                            decoded,
                            "ModelGroup",
                            found.modelGroups
                    );

                    scanToken(
                            decoded,
                            "Model",
                            found.models
                    );

                    scanToken(
                            decoded,
                            "Grade",
                            found.grades
                    );

                    scanToken(
                            decoded,
                            "BadgeGroup",
                            found.badgeGroups
                    );

                    scanToken(
                            decoded,
                            "Badge",
                            found.badges
                    );

                    scanToken(
                            decoded,
                            "FuelType",
                            found.fuels
                    );
                }

            } catch (
                    Exception ignored
            ) {

            }
        }


        scanToken(
                text,
                "ModelGroup",
                found.modelGroups
        );


        scanToken(
                text,
                "Model",
                found.models
        );


        scanToken(
                text,
                "Grade",
                found.grades
        );


        scanToken(
                text,
                "BadgeGroup",
                found.badgeGroups
        );


        scanToken(
                text,
                "Badge",
                found.badges
        );


        scanToken(
                text,
                "FuelType",
                found.fuels
        );
    }


    private void scanToken(
            String text,
            String token,
            Set<String> output
    ) {

        String marker =
                token
                        +
                ".";


        int searchFrom =
                0;


        while (
                true
        ) {

            int index =
                    text.indexOf(
                            marker,
                            searchFrom
                    );


            if (
                    index
                            <
                    0
            ) {

                break;
            }


            int start =
                    index
                            +
                    marker.length();


            int end =
                    findTokenEnd(
                            text,
                            start
                    );


            if (
                    end
                            >
                    start
            ) {

                String value =
                        text.substring(
                                start,
                                end
                        );


                value =
                        cleanCandidate(
                                value
                        );


                if (
                        isGoodCandidate(
                                value
                        )
                ) {

                    output.add(
                            value
                    );
                }
            }


            searchFrom =
                    start;
        }
    }


    /*
     * Търсим края на Encar token,
     * без да режем вътрешни интервали/тирета/скоби.
     */
    private int findTokenEnd(
            String text,
            int start
    ) {

        int best =
                text.length();


        String[] delimiters = {

                "._.",
                "_.",
                ".)",
                "._(",

                ".CarType.",
                ".Manufacturer.",
                ".ModelGroup.",
                ".Model.",
                ".Grade.",
                ".BadgeGroup.",
                ".Badge.",
                ".FuelType.",

                ".Year.",
                ".Mileage.",
                ".Price.",
                ".SellType.",
                ".Separation.",
                ".Color.",
                ".Transmission."
        };


        for (
                String delimiter :
                delimiters
        ) {

            int index =
                    text.indexOf(
                            delimiter,
                            start
                    );


            if (
                    index
                            >=
                    0
                            &&
                    index
                            <
                    best
            ) {

                best =
                        index;
            }
        }


        return best;
    }


    // =========================================================
    // ADD BY TYPE
    // =========================================================

    private void addByType(
            String type,
            String value,
            Found found
    ) {

        String cleaned =
                cleanCandidate(
                        value
                );


        if (
                !isGoodCandidate(
                        cleaned
                )
        ) {

            return;
        }


        if (
                "ModelGroup".equals(
                        type
                )
        ) {

            found.modelGroups.add(
                    cleaned
            );


        } else if (
                "Model".equals(
                        type
                )
        ) {

            found.models.add(
                    cleaned
            );


        } else if (
                "Grade".equals(
                        type
                )
        ) {

            found.grades.add(
                    cleaned
            );


        } else if (
                "BadgeGroup".equals(
                        type
                )
        ) {

            found.badgeGroups.add(
                    cleaned
            );


        } else if (
                "Badge".equals(
                        type
                )
        ) {

            found.badges.add(
                    cleaned
            );


        } else if (
                "FuelType".equals(
                        type
                )
        ) {

            found.fuels.add(
                    cleaned
            );
        }
    }


    // =========================================================
    // TYPE HELPERS
    // =========================================================

    private boolean isCatalogType(
            String value
    ) {

        if (
                value
                        ==
                null
        ) {

            return false;
        }


        String v =
                value.trim();


        return v.equalsIgnoreCase(
                "ModelGroup"
        )
                ||
                v.equalsIgnoreCase(
                        "Model"
                )
                ||
                v.equalsIgnoreCase(
                        "Grade"
                )
                ||
                v.equalsIgnoreCase(
                        "BadgeGroup"
                )
                ||
                v.equalsIgnoreCase(
                        "Badge"
                )
                ||
                v.equalsIgnoreCase(
                        "FuelType"
                );
    }


    private String normalizeType(
            String value
    ) {

        if (
                value.equalsIgnoreCase(
                        "ModelGroup"
                )
        ) {

            return "ModelGroup";
        }


        if (
                value.equalsIgnoreCase(
                        "Model"
                )
        ) {

            return "Model";
        }


        if (
                value.equalsIgnoreCase(
                        "Grade"
                )
        ) {

            return "Grade";
        }


        if (
                value.equalsIgnoreCase(
                        "BadgeGroup"
                )
        ) {

            return "BadgeGroup";
        }


        if (
                value.equalsIgnoreCase(
                        "Badge"
                )
        ) {

            return "Badge";
        }


        if (
                value.equalsIgnoreCase(
                        "FuelType"
                )
        ) {

            return "FuelType";
        }


        return null;
    }


    private boolean isNameField(
            String key
    ) {

        if (
                key
                        ==
                null
        ) {

            return false;
        }


        return key.equalsIgnoreCase(
                "Value"
        )
                ||
                key.equalsIgnoreCase(
                        "Name"
                )
                ||
                key.equalsIgnoreCase(
                        "Text"
                )
                ||
                key.equalsIgnoreCase(
                        "Label"
                )
                ||
                key.equalsIgnoreCase(
                        "Title"
                )
                ||
                key.equalsIgnoreCase(
                        "DisplayName"
                );
    }


    // =========================================================
    // CANDIDATE CLEANUP
    // =========================================================

    private String cleanCandidate(
            String value
    ) {

        if (
                value
                        ==
                null
        ) {

            return "";
        }


        String result =
                value.trim();


        while (
                result.endsWith(
                        "."
                )
                        ||
                result.endsWith(
                        ")"
                )
                        ||
                result.endsWith(
                        "\""
                )
                        ||
                result.endsWith(
                        "'"
                )
        ) {

            result =
                    result.substring(
                            0,
                            result.length()
                                    -
                            1
                    )
                            .trim();
        }


        while (
                result.startsWith(
                        "\""
                )
                        ||
                result.startsWith(
                        "'"
                )
        ) {

            result =
                    result.substring(
                            1
                    )
                            .trim();
        }


        return result;
    }


    private boolean isGoodCandidate(
            String value
    ) {

        if (
                value
                        ==
                null
        ) {

            return false;
        }


        String v =
                value.trim();


        if (
                v.isEmpty()
                        ||
                v.length()
                        >
                100
        ) {

            return false;
        }


        if (
                v.equalsIgnoreCase(
                        "Model"
                )
                        ||
                v.equalsIgnoreCase(
                        "ModelGroup"
                )
                        ||
                v.equalsIgnoreCase(
                        "Grade"
                )
                        ||
                v.equalsIgnoreCase(
                        "Badge"
                )
                        ||
                v.equalsIgnoreCase(
                        "BadgeGroup"
                )
                        ||
                v.equalsIgnoreCase(
                        "FuelType"
                )
        ) {

            return false;
        }


        if (
                v.startsWith(
                        "http"
                )
                        ||
                v.contains(
                        "{"
                )
                        ||
                v.contains(
                        "}"
                )
                        ||
                v.contains(
                        "["
                )
                        ||
                v.contains(
                        "]"
                )
        ) {

            return false;
        }


        /*
         * Само число = вероятно count/id.
         */
        if (
                v.matches(
                        "[0-9,.]+"
                )
        ) {

            return false;
        }


        return true;
    }


    private void cleanupSet(
            Set<String> set
    ) {

        Set<String> cleaned =
                new LinkedHashSet<>();


        for (
                String item :
                set
        ) {

            String value =
                    cleanCandidate(
                            item
                    );


            if (
                    isGoodCandidate(
                            value
                    )
            ) {

                cleaned.add(
                        value
                );
            }
        }


        set.clear();

        set.addAll(
                cleaned
        );
    }


    // =========================================================
    // STRUCTURED LOG
    // =========================================================

    private void writeFound(
            Task task,
            Found found
    ) throws Exception {

        for (
                String value :
                found.modelGroups
        ) {

            writeCatalogLine(
                    "MODEL_GROUP",
                    task,
                    value
            );
        }


        for (
                String value :
                found.models
        ) {

            writeCatalogLine(
                    "MODEL",
                    task,
                    value
            );
        }


        for (
                String value :
                found.grades
        ) {

            writeCatalogLine(
                    "GRADE",
                    task,
                    value
            );
        }


        for (
                String value :
                found.badgeGroups
        ) {

            writeCatalogLine(
                    "BADGE_GROUP",
                    task,
                    value
            );
        }


        for (
                String value :
                found.badges
        ) {

            writeCatalogLine(
                    "BADGE",
                    task,
                    value
            );
        }


        for (
                String value :
                found.fuels
        ) {

            writeCatalogLine(
                    "FUEL",
                    task,
                    value
            );
        }
    }


    private void writeCatalogLine(
            String type,
            Task task,
            String value
    ) throws Exception {

        String line =
                "CATALOG"
                        +
                "|BRAND="
                        +
                task.brand.display
                        +
                "|MANUFACTURER="
                        +
                task.brand.manufacturer
                        +
                "|MODEL_GROUP="
                        +
                safe(
                        task.modelGroup
                )
                        +
                "|MODEL="
                        +
                safe(
                        task.model
                )
                        +
                "|GRADE="
                        +
                safe(
                        task.grade
                )
                        +
                "|BADGE_GROUP="
                        +
                safe(
                        task.badgeGroup
                )
                        +
                "|FOUND_TYPE="
                        +
                type
                        +
                "|VALUE="
                        +
                value;


        writeLine(
                line
                        +
                "\n"
        );


        appendSummary(
                line
        );
    }


    private String safe(
            String value
    ) {

        return value
                ==
        null
                ?
        ""
                :
        value;
    }


    // =========================================================
    // RAW RESPONSE LOG
    // =========================================================

    private void writeRawResponse(
            int number,
            Task task,
            String q,
            ApiResult response
    ) throws Exception {

        writeLine(
                "\n\n"
        );


        writeLine(
                "============================================================\n"
        );


        writeLine(
                "REQUEST #"
                        +
                number
                        +
                "\n"
        );


        writeLine(
                "BRAND: "
                        +
                task.brand.display
                        +
                "\n"
        );


        writeLine(
                "TASK: "
                        +
                taskDescription(
                        task
                )
                        +
                "\n"
        );


        writeLine(
                "HTTP: "
                        +
                response.code
                        +
                "\n"
        );


        writeLine(
                "Q:\n"
                        +
                q
                        +
                "\n"
        );


        writeLine(
                "URL:\n"
                        +
                response.url
                        +
                "\n"
        );


        writeLine(
                "---------------- RAW RESPONSE ----------------\n"
        );


        writeLine(
                response.body
                        ==
                null
                        ?
                ""
                        :
                response.body
        );


        writeLine(
                "\n-------------- END RAW RESPONSE --------------\n"
        );
    }


    // =========================================================
    // OUTPUT FILE
    // =========================================================

    private void openOutputFile()
            throws Exception {

        String stamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                )
                        .format(
                                new Date()
                        );


        outputFileName =
                "ENCAR_CATALOG_SCAN_ALL_"
                        +
                stamp
                        +
                ".txt";


        if (
                Build.VERSION.SDK_INT
                        >=
                Build.VERSION_CODES.Q
        ) {

            ContentValues values =
                    new ContentValues();


            values.put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    outputFileName
            );


            values.put(
                    MediaStore.Downloads.MIME_TYPE,
                    "text/plain"
            );


            values.put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                            +
                    "/"
            );


            downloadUri =
                    getContentResolver()
                            .insert(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    values
                            );


            if (
                    downloadUri
                            ==
                    null
            ) {

                throw new Exception(
                        "Не мога да създам файла в Downloads."
                );
            }


            OutputStream stream =
                    getContentResolver()
                            .openOutputStream(
                                    downloadUri
                            );


            if (
                    stream
                            ==
                    null
            ) {

                throw new Exception(
                        "Не мога да отворя output файла."
                );
            }


            fileWriter =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
                    );


        } else {

            File folder =
                    getExternalFilesDir(
                            Environment.DIRECTORY_DOWNLOADS
                    );


            if (
                    folder
                            ==
                    null
            ) {

                folder =
                        getExternalFilesDir(
                                null
                        );
            }


            File file =
                    new File(
                            folder,
                            outputFileName
                    );


            fallbackFilePath =
                    file.getAbsolutePath();


            fileWriter =
                    new BufferedWriter(
                            new FileWriter(
                                    file
                            )
                    );
        }
    }


    private void writeHeader()
            throws Exception {

        writeLine(
                "===== ENCAR AUTO CATALOG SCAN =====\n"
        );


        writeLine(
                "DATE="
                        +
                new Date()
                        +
                "\n"
        );


        writeLine(
                "BRANDS=KIA,HYUNDAI,MERCEDES,BMW,AUDI\n"
        );


        writeLine(
                "API="
                        +
                API
                        +
                "\n"
        );


        writeLine(
                "DELAY_MS="
                        +
                REQUEST_DELAY_MS
                        +
                "\n"
        );


        writeLine(
                "MAX_REQUESTS="
                        +
                MAX_REQUESTS
                        +
                "\n"
        );


        writeLine(
                "===================================\n\n"
        );
    }


    private synchronized void writeLine(
            String text
    ) throws Exception {

        if (
                fileWriter
                        ==
                null
        ) {

            return;
        }


        fileWriter.write(
                text
        );


        /*
         * Flush след всяка заявка/ред.
         *
         * Ако приложението бъде затворено,
         * вече събраното остава във файла.
         */
        fileWriter.flush();
    }


    private void closeOutputFile() {

        try {

            if (
                    fileWriter
                            !=
                    null
            ) {

                fileWriter.flush();

                fileWriter.close();
            }

        } catch (
                Exception ignored
        ) {

        }


        fileWriter =
                null;
    }


    private String getSavedLocation() {

        if (
                Build.VERSION.SDK_INT
                        >=
                Build.VERSION_CODES.Q
        ) {

            return "Файл: Downloads/"
                    +
                    outputFileName;
        }


        return "Файл: "
                +
                fallbackFilePath;
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    private synchronized void appendSummary(
            String line
    ) {

        /*
         * Обобщението може да стане голямо,
         * но е много по-малко от RAW файла.
         */
        summary.append(
                line
        );

        summary.append(
                "\n"
        );


        /*
         * Не обновяваме TextView при всеки намерен token,
         * защото ще забави сканирането.
         */
    }


    private void copySummary() {

        String text =
                summary.toString();


        if (
                text.isEmpty()
        ) {

            text =
                    status
                            .getText()
                            .toString();
        }


        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                Context.CLIPBOARD_SERVICE
                        );


        clipboard.setPrimaryClip(
                ClipData.newPlainText(
                        "Encar catalog summary",
                        text
                )
        );


        Toast.makeText(
                this,
                "Обобщението е копирано.",
                Toast.LENGTH_SHORT
        ).show();
    }


    // =========================================================
    // STATUS
    // =========================================================

    private void showProgress(
            String message
    ) {

        runOnUiThread(
                () -> {

                    status.setText(
                            message
                    );


                    /*
                     * Показваме само последните части от summary.
                     */
                    String all =
                            summary.toString();


                    if (
                            all.length()
                                    >
                            12000
                    ) {

                        all =
                                all.substring(
                                        all.length()
                                                -
                                        12000
                                );
                    }


                    outputView.setText(
                            all
                    );
                }
        );
    }


    private String taskDescription(
            Task task
    ) {

        StringBuilder text =
                new StringBuilder();


        text.append(
                task.brand.display
        );


        if (
                task.modelGroup
                        !=
                null
        ) {

            text.append(
                    " → ModelGroup: "
            );


            text.append(
                    task.modelGroup
            );
        }


        if (
                task.model
                        !=
                null
        ) {

            text.append(
                    " → Model: "
            );


            text.append(
                    task.model
            );
        }


        if (
                task.grade
                        !=
                null
        ) {

            text.append(
                    " → Grade: "
            );


            text.append(
                    task.grade
            );
        }


        if (
                task.badgeGroup
                        !=
                null
        ) {

            text.append(
                    " → BadgeGroup: "
            );


            text.append(
                    task.badgeGroup
            );
        }


        return text.toString();
    }


    // =========================================================
    // HTTP HELPERS
    // =========================================================

    private String encode(
            String value
    ) throws Exception {

        return URLEncoder.encode(
                value,
                "UTF-8"
        );
    }


    private String readAll(
            InputStream stream
    ) throws Exception {

        if (
                stream
                        ==
                null
        ) {

            return "";
        }


        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
                );


        StringBuilder result =
                new StringBuilder();


        String line;


        while (
                (
                        line =
                                reader.readLine()
                )
                        !=
                null
        ) {

            result.append(
                    line
            );


            result.append(
                    "\n"
            );
        }


        reader.close();


        return result.toString();
    }


    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        stopRequested =
                true;


        closeOutputFile();


        if (
                cookieWebView
                        !=
                null
        ) {

            cookieWebView.stopLoading();

            cookieWebView.destroy();
        }


        super.onDestroy();
    }
}
