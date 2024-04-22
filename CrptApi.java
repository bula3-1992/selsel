import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.web.bind.annotation.PostMapping;


public class CrptApi {
    //декларативим внешний ресурс
    public interface FooClient {
        @PostMapping("/api/v3/lk/documents/create/")
        void send(ObmenDto dto);
    }

    //public static void main () нужно как то заставить работать с объектом класса
    public static class NonStaticContext {
        private FooClient fooClient;
        //очередь - я честно не понял постановку задачи и решил не замарачиваться
        //ведь есть готовая очередь, что вам еще надо?
        private ExecutorService executorService;
        //парамерты threadPoolSize - размер очереди и timeUnit - время ожидания
        NonStaticContext(int threadPoolSize, long timeUnit) {
            executorService =
                    new ThreadPoolExecutor(threadPoolSize, threadPoolSize, timeUnit, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>());
        }
        //обертка вокруг Runnable, чтобы ExecutorService мог переварить наши вызовы
        class ThreadTask implements Runnable {
            ObmenDto dto;
            ThreadTask(ObmenDto dto) { this.dto = dto; }
            public void run() {
                //Feign отправит все за нас
                try {
                    fooClient.send(dto);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //главный вход
        public void doTest() {
            //собираем http клиент (Feign) для работы с JSON через ObjectMapper, тут можно указать URL
            this.fooClient = Feign.builder()
                    .encoder(new JacksonEncoder(new ObjectMapper()))
                    .decoder(new JacksonDecoder(new ObjectMapper()))
                    .contract(new SpringMvcContract())
                    .target(FooClient.class, "https://ismp.crpt.ru/");
            ObmenDto dto = new ObmenDto();
            //dto.setValues... какие то атрибуты
            ThreadTask task = new ThreadTask(dto);
            //вызываем runnable, который отправит данные на ресурс
            executorService.submit(task);
        }

        //допустим это рабочий класс, этот метод будет принимать dto извне и класть их в очередь
        public void acceptDto(ObmenDto dto) {
            ThreadTask task = new ThreadTask(dto);
            executorService.submit(task);
        }

        //выходим из теста
        public void stopTest() {
            executorService.shutdown();
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello");
        NonStaticContext nsc = new NonStaticContext(3, 0L);
        nsc.doTest();
        nsc.stopTest();
    }

    //POJO для обмена без get set для лаконичности
    public static class ObmenDto {
        enum DocType {
            LP_INTRODUCE_GOODS,
            OTHER
        }
        static class Description {
            String participantInn;
        }
        static class Product {
            String certificate_document;
            @JsonFormat(pattern = "yyyy-MM-dd")
            Date certificate_document_date;
            String certificate_document_number;
            String owner_inn;
            String producer_inn;
            @JsonFormat(pattern = "yyyy-MM-dd")
            Date production_date;
            String tnved_code;
            String uit_code;
            String uitu_code;
        }
        Description description;
        String doc_id;
        String doc_status;
        DocType doc_type;
        Boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        @JsonFormat(pattern = "yyyy-MM-dd")
        Date production_date;
        String production_type;
        List<Product> products = new ArrayList<>();
        @JsonFormat(pattern = "yyyy-MM-dd")
        Date reg_date;
        String reg_number;
    }
}
