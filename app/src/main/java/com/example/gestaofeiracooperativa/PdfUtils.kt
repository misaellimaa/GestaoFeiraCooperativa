package com.example.gestaofeiracooperativa // Seu package

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun compartilharPdf(
    context: Context, // <<< Adicionado parâmetro de Contexto
    pdfFile: File,
    authority: String, // Ex: "com.example.gestaofeiracooperativa.provider"
    chooserTitle: String
) {
    if (pdfFile.exists() && pdfFile.canRead()) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                authority, // Usa a autoridade passada
                pdfFile
            )

            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    } else {
        Toast.makeText(context, "Arquivo PDF não encontrado ou não pode ser lido.", Toast.LENGTH_SHORT).show()
    }
}
