package com.Invetory.mangment.Inventory.Order.Management.service;

import com.Invetory.mangment.Inventory.Order.Management.entity.*;
import com.Invetory.mangment.Inventory.Order.Management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        OrderItemRepository orderItemRepository,
                        OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    public Order createOrder(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        return orderRepository.save(order);
    }

    public Order addItemToOrder(Long orderId, Long productId, int quantity) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Can only add items to DRAFT orders");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        orderItemRepository.save(item);
        return order;
    }

    public Order removeItemFromOrder(Long orderId, Long itemId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Can only remove items from DRAFT orders");
        }
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        orderItemRepository.delete(item);
        return order;
    }

    public Order confirmOrder(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT orders can be confirmed");
        }

        // First pass: validate all stock before touching anything
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getName());
            }
        }

        // Second pass: deduct stock, lock in unit price, accumulate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
            item.setUnitPrice(product.getPrice());
            orderItemRepository.save(item);
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        OrderStatus oldStatus = order.getStatus();
        order.setTotalPrice(total);
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        saveHistory(saved, oldStatus, OrderStatus.CONFIRMED);
        return saved;
    }

    public Order cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();
        if (oldStatus == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        } else if (oldStatus != OrderStatus.DRAFT) {
            throw new RuntimeException("Cannot cancel order with status: " + oldStatus);
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        saveHistory(saved, oldStatus, OrderStatus.CANCELLED);
        return saved;
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order order = getOrderById(orderId);
        OrderStatus current = order.getStatus();
        OrderStatus next = OrderStatus.valueOf(status);
        if (current == OrderStatus.CONFIRMED && next == OrderStatus.SHIPPED) {
            order.setStatus(OrderStatus.SHIPPED);
        } else if (current == OrderStatus.SHIPPED && next == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
        } else {
            throw new RuntimeException("Invalid status transition: " + current + " -> " + next);
        }
        Order saved = orderRepository.save(order);
        saveHistory(saved, current, next);
        return saved;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderOrdId(orderId);
    }

    private void saveHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setChangedAt(LocalDateTime.now());
        orderStatusHistoryRepository.save(history);
    }
}
