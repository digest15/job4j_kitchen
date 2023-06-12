package ru.job4j.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.job4j.domain.Order;
import ru.job4j.domain.OrderStatus;
import ru.job4j.repository.OrderStatusRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka-order-status-topic}")
    private String orderStatusTopicName;

    @Override
    public List<OrderStatus> findAll() {
        return orderStatusRepository.findAll();
    }

    @Override
    public Optional<OrderStatus> findById(int id) {
        return orderStatusRepository.findById(id);
    }

    @Override
    public List<OrderStatus> findByOrderId(int id) {
        return orderStatusRepository.findByOrderId(new Order(id));
    }

    @Override
    public Optional<OrderStatus> findLastByOrderId(int id) {
        return orderStatusRepository.findLastByOrderIdOrderByCreationDate(id);
    }

    @Override
    public Optional<OrderStatus> save(OrderStatus orderStatus) {
        Optional<OrderStatus> optOrderStatus = Optional.empty();
        try {
            orderStatusRepository.save(new OrderStatus(orderStatus.getOrderId(), orderStatus.getStatus()));
            optOrderStatus = Optional.of(orderStatus);
        } catch (Exception e) {
            log.error("Save or Update was wrong", e);
        }

        optOrderStatus.ifPresent(status ->
                kafkaTemplate.send(orderStatusTopicName, status)
        );

        return optOrderStatus;
    }

    @Override
    public boolean delete(int id) {
        Optional<OrderStatus> orderStatus = orderStatusRepository.findById(id);
        orderStatus.ifPresent(orderStatusRepository::delete);
        return orderStatus.isPresent();
    }
}
