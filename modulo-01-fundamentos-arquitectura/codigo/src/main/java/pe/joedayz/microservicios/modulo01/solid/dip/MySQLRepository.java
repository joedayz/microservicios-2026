package pe.joedayz.microservicios.modulo01.solid.dip;

public class MySQLRepository  implements OrderRepository {

    @Override
    public void saveOrder(Order order) {
        System.out.println("Guardando orden en MySQL");
    }



}
