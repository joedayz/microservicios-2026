package pe.joedayz.microservicios.modulo01.solid.liskov;

public class FakeGateway implements PaymentGateway {

    @Override
    public void pay(double amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
