package com.anastasia.Anastasia_BackEnd.core.auth.support;

public interface ActivationTokenObserver {

    void record(String email, String rawToken);
}
