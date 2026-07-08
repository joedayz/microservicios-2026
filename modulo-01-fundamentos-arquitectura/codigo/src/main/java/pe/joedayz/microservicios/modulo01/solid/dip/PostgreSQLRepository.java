package pe.joedayz.microservicios.modulo01.solid.dip;

public class PostgreSQLRepository implements OrderRepository {

    @Override
    public void saveOrder(Order order) {
        System.out.println("Guardando orden en PostgreSQL");
    }
}
