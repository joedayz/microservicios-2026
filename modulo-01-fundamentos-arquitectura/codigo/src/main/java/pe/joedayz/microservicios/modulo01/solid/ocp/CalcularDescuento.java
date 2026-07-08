package pe.joedayz.microservicios.modulo01.solid.ocp;

public class CalcularDescuento {

    double descuento;

    public double calcular(double total, String tipoCliente) {
        if(tipoCliente.equals("ESTUDIANTE")){
            descuento = total * 0.10;
        }
        else if(tipoCliente.equals("JUBILADO")){
            descuento = total * 0.15;
        }
        else if(tipoCliente.equals("VIP")){
            descuento = total * 0.20;
        }
        return descuento;
    }
}
