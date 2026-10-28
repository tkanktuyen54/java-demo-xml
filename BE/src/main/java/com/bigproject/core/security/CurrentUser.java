package com.bigproject.core.security;

import java.io.Serializable;

public record CurrentUser(long id, String email) implements Serializable {}
