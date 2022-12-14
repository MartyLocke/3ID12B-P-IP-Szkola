package pl.hefajstos.Klasy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.hefajstos.Nauczyciele.Nauczyciele_view;
import pl.hefajstos.hefajstos.QuickJSONArray;
import pl.hefajstos.uczen.Uczen;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@RestController
public class KlasyController
{
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @GetMapping("/klasy/uczniowie/{sid}/{klasa}")
    public String getListaKlasy (@PathVariable("sid") String sid, @PathVariable("klasa") String klasa)
    {
        String sql = String.format("SELECT * FROM Uczen WHERE Klasa = '%s' ORDER BY Nazwisko, Imie", klasa);

        List<Uczen> uczniowie = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Uczen.class));

        QuickJSONArray q = new QuickJSONArray("lista");
        for (Uczen u : uczniowie)
            q.add(u.toString());

        return q.ret();
    }

    @GetMapping("/klasy/lista/{sid}")
    public String getListaKlas (@PathVariable("sid") String sid)
    {
        String sql = "SELECT * FROM Klasy_view";
        List<Klasy_view> klasy = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Klasy_view.class));
        QuickJSONArray q = new QuickJSONArray("klasy");
        for (Klasy_view k : klasy)
            q.add(k.toString());
        return q.ret();
    }

    @GetMapping("/klasy/generuj/agresywnie/{sid}/{min}/{max}")
    public String getNauczyciele (@PathVariable("sid") String sid, @PathVariable("min") String min, @PathVariable("max") String max)
    {
        Integer cMin = Integer.valueOf(min);
        Integer cMax = Integer.valueOf(max);

        String sql = "SELECT * FROM Nauczyciele_view";
        List<Nauczyciele_view> nauczyciele = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Nauczyciele_view.class));

        sql = "SELECT * FROM Uczen";
        List<Uczen> uczniowie = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Uczen.class));

        /*
            0. Usuni??cie wszystkich klas oraz ich po??cze??
         */

        jdbcTemplate.execute("UPDATE Uczen SET Klasa = '0'");
        jdbcTemplate.execute("UPDATE Nauczyciel SET Klasa_Id = '_'");
        jdbcTemplate.execute("DELETE FROM Klasa WHERE Nazwa != '0'");

        String buff = "";

        /*
            1. Podzia?? na roczniki
         */

        ArrayList<Uczen>[] rocznik = new ArrayList[8];

        for (int i = 0; i < 8; i++)
            rocznik[i] = new ArrayList<Uczen>();

        Integer obecnyRok = Year.now().getValue();
        for (Uczen u : uczniowie)
        {
            Integer poziom = obecnyRok - u.getData_urodzenia().toLocalDate().getYear() - 7;
            if (poziom < 0)
                poziom = 0;
            if (poziom > 7)
                poziom = 7;
            rocznik[poziom].add(u);
        }

        /*
            Podzia?? na klasy w rocznikach
            |max - min| - najbardziej optymalny podzia?? wg. dyrektora
         */


        Integer[] ilosciKlas = new Integer[8];
        for (int i = 0; i < 8; i++)
            ilosciKlas[i] = 0;

        Integer optymalnyPodzial = cMax - 1;

        boolean warunkiSpelnione = false;
        Integer bezpiecznik = 100;

        do
        {
            if (bezpiecznik-- <= 0)
            {
                break;
            }
            Integer przydzieleniWychowawcy = 0;
            if (optymalnyPodzial <= 0)
                optymalnyPodzial = cMin;

            buff += "<BR><B>PASS " + bezpiecznik + "</B><BR>";

            for (int i = 0; i < 8; i++)
            {
                Integer iloscUczniow = rocznik[i].size();
                Integer iloscKlas = (int) Math.ceil((float) iloscUczniow / (float) optymalnyPodzial);
                Integer uczniowieNaKlase = (int) Math.ceil((float) iloscUczniow / (float) iloscKlas);
                ilosciKlas[i] = iloscKlas;

                buff += "Klasa " + (i + 1) + " -> " + iloscKlas + " ucz/klasa: " + (uczniowieNaKlase) + " ;w rocz: " + iloscUczniow + "<BR>";
                przydzieleniWychowawcy += iloscKlas;
            }
            buff += "Wych: " + przydzieleniWychowawcy + " z " + nauczyciele.size() + "<br>";
            if (przydzieleniWychowawcy <= 0)
            {
                optymalnyPodzial--;
            }
            else if (przydzieleniWychowawcy <= nauczyciele.size())
                warunkiSpelnione = true;
            else
                optymalnyPodzial++;
        }
        while(warunkiSpelnione == false);

        /*
            Utw??rz Nowe klasy wg. optymalnego podzia??u
            ORAZ przypisanie wychowawc??w
         */

        int nauczyciel_idx = 0;
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < ilosciKlas[i]; j++)
            {
                String klasa = (i + 1) + (new Character((char)((char) 'A' + j))).toString();
                sql = String.format("INSERT INTO Klasa VALUES ('%s', %s)", klasa, i + 1);
                jdbcTemplate.execute(sql);

                sql = String.format("UPDATE Nauczyciel SET Klasa_Id = '%s' WHERE Id = '%s'", klasa, nauczyciele.get(nauczyciel_idx++).getNauczyciel_Id());
                jdbcTemplate.execute(sql);
            }
        }

        /*
            U??ywaj??c optymalnegoPodzia??u, Przypisz kolejnych uczni??w
            z rocznik??w do ich nowych klas!
         */

        for (int i = 0; i < 8; i++)
        {
            int uczenOffset = 0;
            Integer iloscUczniow = rocznik[i].size();
            Integer iloscKlas = (int) Math.ceil((float) iloscUczniow / (float) optymalnyPodzial);
            Integer uczniowieNaKlase = (int) Math.ceil((float) iloscUczniow / (float) iloscKlas);
            for (int j = 0; j < ilosciKlas[i]; j++)
            {
                String klasa = (i + 1) + (new Character((char)((char) 'A' + j))).toString();
                // Przypisac kolejne [uczenOffset, czenOffset + uczniowieNaKlase] do klasy (i+1)'CH'
                for (int k = 0; k < uczniowieNaKlase; k++)
                {
                    int uczen_idx = uczenOffset + k;
                    if (uczen_idx >= rocznik[i].size())
                        break;
                    Uczen uczen = rocznik[i].get(uczen_idx);

                    String usql = String.format("UPDATE Uczen SET Klasa = '%s', Numer = %s WHERE Id = '%s'", klasa, "" + (k + 1), uczen.getId());
                    jdbcTemplate.execute(usql);
                }
                uczenOffset += uczniowieNaKlase;
            }
        }

        return buff;
    }

    /*
        (!) Algorytm Generacji Klas

        1. podzia?? na roczniki
        roczniki[] := (_/ 1 klasa)

        2. podzia?? na mniejsze klasy
        _/ rocznika -> ilosc / MAX_UCZNI??W
        -> roczniki[] := [[], [], ..]

        3. optymalizacjia!

        IF *(roczniki) > *(nauczyciel) -> assertFALSE
            // wali?? ograniczenie dyrektora,  nie da si?? inaczej

        FOR rok : roczniki
            FOR klasa : rok
                IF *(klasa) > MAX_UCZ ->
                    CZY MO??NA PODZIELI??? {
                        CZY MAMY WYSTARCZAJ??CO DU??O NAUCZYCIELI?
                    }
     */
}
