package com.bskai.models

data class ModelCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val fileName: String,
    val url: String,
    val sizeHint: String,
    val parameters: String,
    val quant: String
)

/**
 * 内置可下载的本地 GGUF 模型清单。
 */
object ModelCatalog {
    val entries: List<ModelCatalogEntry> = listOf(
        ModelCatalogEntry(
            id = "smollm2-135m",
            name = "SmolLM2-135M-Instruct",
            description = "轻量指令模型，适合低端设备推理，速度极快",
            fileName = "SmolLM2-135M-Instruct-Q8_0.gguf",
            url = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q8_0.gguf",
            sizeHint = "约 146 MB",
            parameters = "135M",
            quant = "Q8_0"
        ),
        ModelCatalogEntry(
            id = "qwen2.5-0.5b",
            name = "Qwen2.5-0.5B-Instruct",
            description = "通义千问 0.5B，中文能力均衡，中端设备可流畅运行",
            fileName = "qwen2.5-0.5b-instruct-q5_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q5_k_m.gguf",
            sizeHint = "约 370 MB",
            parameters = "0.5B",
            quant = "Q5_K_M"
        ),
        ModelCatalogEntry(
            id = "qwen2.5-1.5b",
            name = "Qwen2.5-1.5B-Instruct",
            description = "通义千问 1.5B，中文能力更强，旗舰设备可运行",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeHint = "约 940 MB",
            parameters = "1.5B",
            quant = "Q4_K_M"
        ),
        ModelCatalogEntry(
            id = "llama3.2-1b",
            name = "Llama-3.2-1B-Instruct",
            description = "Meta Llama 3.2 1B，英文指令模型，通用性强",
            fileName = "Llama-3.2-1B-Instruct-Q8_0.gguf",
            url = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q8_0.gguf",
            sizeHint = "约 1.1 GB",
            parameters = "1B",
            quant = "Q8_0"
        ),
        ModelCatalogEntry(
            id = "gemma2-2b",
            name = "Gemma-2-2B-it",
            description = "Google Gemma 2 2B，多语言能力均衡",
            fileName = "gemma-2-2b-it-Q8_0.gguf",
            url = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q8_0.gguf",
            sizeHint = "约 2.2 GB",
            parameters = "2B",
            quant = "Q8_0"
        ),
        ModelCatalogEntry(
            id = "tinyllama-1.1b",
            name = "TinyLlama-1.1B-Chat",
            description = "TinyLlama 1.1B 对话模型，体积小部署灵活",
            fileName = "tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf",
            url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf",
            sizeHint = "约 750 MB",
            parameters = "1.1B",
            quant = "Q5_K_M"
        )
    )
}
