package org.example.walletservice.jms.publisher;


import org.springframework.stereotype.Service;


public interface EventPublisher<T> {

    public void publish (T event,String topic);
}
