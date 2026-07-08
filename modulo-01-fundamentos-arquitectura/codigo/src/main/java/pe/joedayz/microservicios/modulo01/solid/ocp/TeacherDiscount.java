package pe.joedayz.microservicios.modulo01.solid.ocp;

public class TeacherDiscount implements MyDiscountRule {
    @Override
    public double calculate(double total) {
        return total * 0.12;
    }
}
