package рбпо.гришин.лаба2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ПроверкаApiTest {
    private static final String СПЕКТАКЛЬ =
            "{\"название\":\"Ревизор\",\"длительностьМинут\":120,\"возрастноеОграничение\":12}";
    private static final String ЗАЛ = "{\"название\":\"Большой\",\"числоМест\":10}";
    private static final String ПОЛЬЗОВАТЕЛЬ = "{\"имя\":\"Никита\"}";
    private static final String[] ПУТИ = {
            "/api/plays", "/api/theatre-halls", "/api/performances", "/api/tickets", "/api/users"
    };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void всеПятьОперацийРаботаютДляКаждогоРесурса() throws Exception {
        long спектакль = создать("/api/plays", СПЕКТАКЛЬ);
        long зал = создать("/api/theatre-halls", ЗАЛ);
        long пользователь = создать("/api/users", ПОЛЬЗОВАТЕЛЬ);
        long показ = создать("/api/performances", показ(спектакль, зал,
                LocalDateTime.now().plusDays(30), "ЗАПЛАНИРОВАН"));
        long билет = создать("/api/tickets", билет(показ, 3, пользователь, "БРОНЬ"));
        long[] номера = {спектакль, зал, показ, билет, пользователь};

        for (int i = 0; i < ПУТИ.length; i++) {
            mvc.perform(get(ПУТИ[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(номера[i]));
            mvc.perform(get(ПУТИ[i] + "/" + номера[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(номера[i]));
        }

        отправить(patch("/api/plays/" + спектакль),
                "{\"название\":\"Гамлет\",\"длительностьМинут\":null,\"возрастноеОграничение\":18}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.название").value("Гамлет"))
                .andExpect(jsonPath("$.длительностьМинут").value(120))
                .andExpect(jsonPath("$.возрастноеОграничение").value(18));
        отправить(patch("/api/theatre-halls/" + зал),
                "{\"название\":\"Основной\",\"числоМест\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.название").value("Основной"))
                .andExpect(jsonPath("$.числоМест").value(10));
        отправить(patch("/api/performances/" + показ),
                "{\"базоваяЦена\":700,\"начало\":null,\"idЗала\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.базоваяЦена").value(700))
                .andExpect(jsonPath("$.idЗала").value(зал))
                .andExpect(jsonPath("$.статус").value("ЗАПЛАНИРОВАН"));
        отправить(patch("/api/tickets/" + билет),
                "{\"статус\":\"КУПЛЕН\",\"idПользователя\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.статус").value("КУПЛЕН"))
                .andExpect(jsonPath("$.номерМеста").value(3))
                .andExpect(jsonPath("$.idПользователя").value(пользователь));
        отправить(patch("/api/users/" + пользователь), "{\"имя\":\"Анна\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.имя").value("Анна"));
        отправить(patch("/api/users/" + пользователь), "{\"имя\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.имя").value("Анна"));
        mvc.perform(get("/api/plays/" + спектакль))
                .andExpect(status().isOk()).andExpect(jsonPath("$.название").value("Гамлет"));
        mvc.perform(get("/api/theatre-halls/" + зал))
                .andExpect(status().isOk()).andExpect(jsonPath("$.название").value("Основной"));
        mvc.perform(get("/api/performances/" + показ))
                .andExpect(status().isOk()).andExpect(jsonPath("$.базоваяЦена").value(700));
        mvc.perform(get("/api/tickets/" + билет))
                .andExpect(status().isOk()).andExpect(jsonPath("$.статус").value("КУПЛЕН"));

        // Сначала удаляем зависимые записи, затем записи, на которые они ссылались.
        int[] порядокУдаления = {3, 2, 0, 1, 4};
        for (int i : порядокУдаления) {
            String адрес = ПУТИ[i] + "/" + номера[i];
            mvc.perform(delete(адрес)).andExpect(status().isNoContent()).andExpect(content().string(""));
            mvc.perform(get(адрес)).andExpect(status().isNotFound());
            mvc.perform(delete(адрес)).andExpect(status().isNotFound());
            mvc.perform(get(ПУТИ[i])).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void неверныеДанныеИПосторонниеПоляОтклоняются() throws Exception {
        long спектакль = создать("/api/plays", СПЕКТАКЛЬ);
        long зал = создать("/api/theatre-halls", ЗАЛ);
        long пользователь = создать("/api/users", ПОЛЬЗОВАТЕЛЬ);
        String данныеПоказа = показ(спектакль, зал, LocalDateTime.now().plusDays(30), "ЗАПЛАНИРОВАН");
        long показ = создать("/api/performances", данныеПоказа);
        long билет = создать("/api/tickets", билет(показ, 1, пользователь, "БРОНЬ"));
        long[] номера = {спектакль, зал, показ, билет, пользователь};
        String[] тела = {СПЕКТАКЛЬ, ЗАЛ, данныеПоказа, билет(показ, 2, пользователь, "КУПЛЕН"), ПОЛЬЗОВАТЕЛЬ};

        for (int i = 0; i < ПУТИ.length; i++) {
            отправить(post(ПУТИ[i]), "{}").andExpect(status().isBadRequest());
            for (String поле : new String[]{"id", "лишнееПоле"}) {
                ObjectNode тело = (ObjectNode) json.readTree(тела[i]);
                тело.put(поле, 123);
                отправить(post(ПУТИ[i]), тело.toString()).andExpect(status().isBadRequest());
                отправить(patch(ПУТИ[i] + "/" + номера[i]), тело.toString())
                        .andExpect(status().isBadRequest());
            }
        }

        Map<String, Long> существующие = Map.of(
                "/api/plays", спектакль, "/api/theatre-halls", зал,
                "/api/performances", показ, "/api/tickets", билет, "/api/users", пользователь);
        String[][] неверныеПоля = {
                {"/api/plays", "{\"название\":\" \"}"},
                {"/api/plays", "{\"длительностьМинут\":0}"},
                {"/api/plays", "{\"возрастноеОграничение\":-1}"},
                {"/api/theatre-halls", "{\"название\":\" \"}"},
                {"/api/theatre-halls", "{\"числоМест\":0}"},
                {"/api/performances", "{\"базоваяЦена\":-1}"},
                {"/api/performances", "{\"начало\":\"не дата\"}"},
                {"/api/performances", "{\"статус\":\"НЕИЗВЕСТНО\"}"},
                {"/api/tickets", "{\"номерМеста\":0}"},
                {"/api/tickets", "{\"статус\":\"НЕИЗВЕСТНО\"}"},
                {"/api/users", "{\"имя\":\" \"}"}
        };
        for (String[] случай : неверныеПоля) {
            отправить(patch(случай[0] + "/" + существующие.get(случай[0])), случай[1])
                    .andExpect(status().isBadRequest());
            int индекс = java.util.Arrays.asList(ПУТИ).indexOf(случай[0]);
            ObjectNode полноеТело = (ObjectNode) json.readTree(тела[индекс]);
            полноеТело.setAll((ObjectNode) json.readTree(случай[1]));
            отправить(post(случай[0]), полноеТело.toString()).andExpect(status().isBadRequest());
        }
    }

    @Test
    void связиЗанятыеМестаИРазмерЗалаПроверяются() throws Exception {
        long спектакль = создать("/api/plays", СПЕКТАКЛЬ);
        long зал = создать("/api/theatre-halls", ЗАЛ);
        long пользователь = создать("/api/users", ПОЛЬЗОВАТЕЛЬ);
        LocalDateTime начало = LocalDateTime.now().plusDays(30);
        отправить(post("/api/performances"), показ(999999, зал, начало, "ЗАПЛАНИРОВАН"))
                .andExpect(status().isNotFound());
        отправить(post("/api/performances"), показ(спектакль, 999999, начало, "ЗАПЛАНИРОВАН"))
                .andExpect(status().isNotFound());
        long показ = создать("/api/performances", показ(спектакль, зал, начало, "ЗАПЛАНИРОВАН"));
        отправить(post("/api/tickets"), билет(999999, 1, пользователь, "БРОНЬ"))
                .andExpect(status().isNotFound());
        отправить(post("/api/tickets"), билет(показ, 1, 999999, "БРОНЬ"))
                .andExpect(status().isNotFound());
        отправить(post("/api/tickets"), билет(показ, 11, пользователь, "БРОНЬ"))
                .andExpect(status().isBadRequest());
        long первыйБилет = создать("/api/tickets", билет(показ, 3, пользователь, "БРОНЬ"));
        long второйБилет = создать("/api/tickets", билет(показ, 4, пользователь, "КУПЛЕН"));

        отправить(post("/api/tickets"), билет(показ, 3, пользователь, "КУПЛЕН"))
                .andExpect(status().isConflict());
        отправить(patch("/api/tickets/" + второйБилет), "{\"номерМеста\":3}")
                .andExpect(status().isConflict());
        отправить(patch("/api/tickets/" + первыйБилет), "{\"номерМеста\":11}")
                .andExpect(status().isBadRequest());
        for (String поле : new String[]{"idПоказа", "idПользователя"}) {
            отправить(patch("/api/tickets/" + первыйБилет), "{\"" + поле + "\":999999}")
                    .andExpect(status().isNotFound());
        }
        for (String поле : new String[]{"idСпектакля", "idЗала"}) {
            отправить(patch("/api/performances/" + показ), "{\"" + поле + "\":999999}")
                    .andExpect(status().isNotFound());
        }
        for (String адрес : new String[]{"/api/plays/" + спектакль, "/api/theatre-halls/" + зал,
                "/api/performances/" + показ, "/api/users/" + пользователь}) {
            mvc.perform(delete(адрес)).andExpect(status().isConflict());
            mvc.perform(get(адрес)).andExpect(status().isOk());
        }

        отправить(patch("/api/theatre-halls/" + зал), "{\"числоМест\":2}")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/theatre-halls/" + зал))
                .andExpect(status().isOk()).andExpect(jsonPath("$.числоМест").value(10));
        long маленькийЗал = создать("/api/theatre-halls", "{\"название\":\"Малый\",\"числоМест\":2}");
        отправить(patch("/api/performances/" + показ), "{\"idЗала\":" + маленькийЗал + "}")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/performances/" + показ))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idЗала").value(зал));
        mvc.perform(get("/api/tickets/" + второйБилет))
                .andExpect(status().isOk()).andExpect(jsonPath("$.номерМеста").value(4));
        отправить(patch("/api/theatre-halls/" + зал), "{\"числоМест\":4}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.числоМест").value(4));
    }

    @Test
    void билетыДоступныТолькоНаБудущиеЗапланированныеПоказы() throws Exception {
        long спектакль = создать("/api/plays", СПЕКТАКЛЬ);
        long зал = создать("/api/theatre-halls", ЗАЛ);
        long пользователь = создать("/api/users", ПОЛЬЗОВАТЕЛЬ);
        long будущийПоказ = создать("/api/performances", показ(спектакль, зал,
                LocalDateTime.now().plusDays(30), "ЗАПЛАНИРОВАН"));
        long билет = создать("/api/tickets", билет(будущийПоказ, 1, пользователь, "БРОНЬ"));
        long[] недоступныеПоказы = {
                создать("/api/performances", показ(спектакль, зал,
                        LocalDateTime.now().minusDays(1), "ЗАПЛАНИРОВАН")),
                создать("/api/performances", показ(спектакль, зал,
                        LocalDateTime.now().plusDays(30), "ОТМЕНЁН")),
                создать("/api/performances", показ(спектакль, зал,
                        LocalDateTime.now().plusDays(30), "ЗАВЕРШЁН"))
        };
        for (long показ : недоступныеПоказы) {
            mvc.perform(get("/api/performances/" + показ)).andExpect(status().isOk());
            for (String состояние : new String[]{"БРОНЬ", "КУПЛЕН"}) {
                отправить(post("/api/tickets"), билет(показ, 2, пользователь, состояние))
                        .andExpect(status().isConflict());
            }
            отправить(patch("/api/tickets/" + билет), "{\"idПоказа\":" + показ + "}")
                    .andExpect(status().isConflict());
        }
        mvc.perform(get("/api/tickets/" + билет))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idПоказа").value(будущийПоказ));
        отправить(patch("/api/performances/" + будущийПоказ), "{\"базоваяЦена\":0}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.базоваяЦена").value(0));
    }

    private ResultActions отправить(MockHttpServletRequestBuilder запрос, String тело) throws Exception {
        return mvc.perform(запрос.contentType(APPLICATION_JSON).content(тело));
    }

    private long создать(String путь, String тело) throws Exception {
        String ответ = отправить(post(путь), тело)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode объект = json.readTree(ответ);
        return объект.get("id").asLong();
    }

    private String показ(long спектакль, long зал, LocalDateTime начало, String статус) {
        return """
                {"idСпектакля":%d,"idЗала":%d,"начало":"%s","базоваяЦена":500,"статус":"%s"}
                """.formatted(спектакль, зал, начало.withNano(0), статус);
    }

    private String билет(long показ, int место, long пользователь, String статус) {
        return """
                {"idПоказа":%d,"номерМеста":%d,"idПользователя":%d,"статус":"%s"}
                """.formatted(показ, место, пользователь, статус);
    }
}
