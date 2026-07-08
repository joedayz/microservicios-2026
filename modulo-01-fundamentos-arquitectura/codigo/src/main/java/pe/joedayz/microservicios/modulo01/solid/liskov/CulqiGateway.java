package pe.joedayz.microservicios.modulo01.solid.liskov;

public class CulqiGateway implements PaymentGateway {

    @Override
    public void pay(double amount) {
        System.out.println("Pago realizado con Culqi por: " + amount);
    }
}
