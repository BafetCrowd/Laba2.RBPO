package рбпо.гришин.лаба2.репозиторий;

import org.springframework.stereotype.Repository;
import рбпо.гришин.лаба2.модель.Билет;
import рбпо.гришин.лаба2.модель.Зал;
import рбпо.гришин.лаба2.модель.Показ;
import рбпо.гришин.лаба2.модель.Пользователь;
import рбпо.гришин.лаба2.модель.Спектакль;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Repository
public class РепозиторийТеатра {
    private final LinkedHashMap<Long, Спектакль> спектакли = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Зал> залы = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Показ> показы = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Билет> билеты = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Пользователь> пользователи = new LinkedHashMap<>();

    private long следующийIdСпектакля = 1;
    private long следующийIdЗала = 1;
    private long следующийIdПоказа = 1;
    private long следующийIdБилета = 1;
    private long следующийIdПользователя = 1;

    public List<Спектакль> всеСпектакли() {
        return new ArrayList<>(спектакли.values());
    }

    public Спектакль найтиСпектакль(long id) {
        return спектакли.get(id);
    }

    public Спектакль сохранитьСпектакль(Спектакль спектакль) {
        if (спектакль.id == null) {
            спектакль.id = следующийIdСпектакля++;
        }
        спектакли.put(спектакль.id, спектакль);
        return спектакль;
    }

    public void удалитьСпектакль(long id) {
        спектакли.remove(id);
    }

    public List<Зал> всеЗалы() {
        return new ArrayList<>(залы.values());
    }

    public Зал найтиЗал(long id) {
        return залы.get(id);
    }

    public Зал сохранитьЗал(Зал зал) {
        if (зал.id == null) {
            зал.id = следующийIdЗала++;
        }
        залы.put(зал.id, зал);
        return зал;
    }

    public void удалитьЗал(long id) {
        залы.remove(id);
    }

    public List<Показ> всеПоказы() {
        return new ArrayList<>(показы.values());
    }

    public Показ найтиПоказ(long id) {
        return показы.get(id);
    }

    public Показ сохранитьПоказ(Показ показ) {
        if (показ.id == null) {
            показ.id = следующийIdПоказа++;
        }
        показы.put(показ.id, показ);
        return показ;
    }

    public void удалитьПоказ(long id) {
        показы.remove(id);
    }

    public List<Билет> всеБилеты() {
        return new ArrayList<>(билеты.values());
    }

    public Билет найтиБилет(long id) {
        return билеты.get(id);
    }

    public Билет сохранитьБилет(Билет билет) {
        if (билет.id == null) {
            билет.id = следующийIdБилета++;
        }
        билеты.put(билет.id, билет);
        return билет;
    }

    public void удалитьБилет(long id) {
        билеты.remove(id);
    }

    public List<Пользователь> всеПользователи() {
        return new ArrayList<>(пользователи.values());
    }

    public Пользователь найтиПользователя(long id) {
        return пользователи.get(id);
    }

    public Пользователь сохранитьПользователя(Пользователь пользователь) {
        if (пользователь.id == null) {
            пользователь.id = следующийIdПользователя++;
        }
        пользователи.put(пользователь.id, пользователь);
        return пользователь;
    }

    public void удалитьПользователя(long id) {
        пользователи.remove(id);
    }
}
