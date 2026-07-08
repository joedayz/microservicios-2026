package pe.joedayz.microservicios.modulo01.solid.dip;

public class Main {

    public static void main(String[] args) {


        OrderRepository repository = new MySQLRepository();

        OrderService service = new OrderService(repository);


        OrderRepository repository2 = new PostgreSQLRepository();

        OrderService service2 = new OrderService(repository);
    }
}
