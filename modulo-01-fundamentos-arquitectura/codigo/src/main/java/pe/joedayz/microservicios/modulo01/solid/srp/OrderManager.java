package pe.joedayz.microservicios.modulo01.solid.srp;

public class OrderManager {

    public void processOrder(double amount){

        //1. Calcular el precio
        double total = amount * 1.18;
        System.out.println("Total: " + total);

        //2. Guardarlo en la Bd
        System.out.println("Guardando pedido...");

        //3. Enviar email al cliente
        System.out.println("Enviando correo al cliente...");
    }

    public static void main(String[] args) {


        MyOrderPricingService pricingService = new MyOrderPricingService();
        double total = pricingService.calculateTotal(100);

        MyOrderRepository orderRepository = new MyOrderRepository();
        orderRepository.save(total);

        MyOrderNotificationService notificationService = new MyOrderNotificationService();
        notificationService.sendConfirmation();
    }
}
