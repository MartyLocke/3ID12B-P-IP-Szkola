package pl.hefajstos.hefajstos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pl.hefajstos.autoryzacja.Sesja;
import pl.hefajstos.autoryzacja.SesjaKontroler;

import java.util.List;

@RestController
public class UczniowieController
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SesjaKontroler kontrolerSesji;

    @GetMapping("/lista_studentow")
    public ResponseEntity<String> getTestJSONString ()
    {
        String sql = "SELECT * FROM Uczen";

        List<Student> students = jdbcTemplate.query(sql,
                BeanPropertyRowMapper.newInstance(Student.class));

        String lista = "{\"studenci\": [";
        for (Student stu : students) {
            lista += (new QuickJSON())
                    .add("imie", stu.getImie())
                    .add("nazwisko", stu.getNazwisko())
                    .add("id", "" + stu.getId())
                    .ret() + ((students.indexOf(stu) + 1 < students.toArray().length) ? ", " : "");
        }
        lista += "]}";
        students.forEach(System.out :: println);
        return new ResponseEntity<>(lista, HttpStatus.valueOf(200));
    }
    @GetMapping("/test_sesja/{sid}")
    public ResponseEntity<String> getTestSesja (@PathVariable("sid") String sid)
    {
        Sesja sesja = kontrolerSesji.getRodzajKonta(sid);

        if (sesja == null)
        { /* konto nie istnieje */
            return new ResponseEntity<>("{\"ok\":false}", HttpStatus.valueOf(200));
        }
        /* konto istnieje */

        return new ResponseEntity<>(String.format("{\"ok\":true, \"id\":\"%s\", \"rodzaj\":\"%s\"}",
                sesja.getKlucz(), sesja.getRodzajKonta().toString()), HttpStatus.valueOf(200));
    }

}
