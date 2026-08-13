package com.beat.admin.support

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers

fun <T> any(): T = ArgumentMatchers.any()

fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)

fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()