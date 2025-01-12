val safeValue = remember { mutableStateOf(0f) }
safeValue.value = if (someValue.isNaN()) 0f else someValue
val animation = animateFloatAsState(targetValue = safeValue.value) 