package com.rorokaiiworks.goodidlegame.core.data

import com.rorokaiiworks.goodidlegame.loadTemplates
import com.charleskorn.kaml.AnchorsAndAliases
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

open class DataTable<T : Template> {
    val data = mutableMapOf<String, T>()

    constructor(templates: List<T>) {
        for (template in templates) {
            data[template.id] = template
        }
    }

    companion object {
        val yaml = Yaml(
            configuration = YamlConfiguration(
                anchorsAndAliases = AnchorsAndAliases.Permitted(99999u),
            )
        )

        suspend inline fun <reified T : Template> createTemplates(
            resourceLoader: IResourceLoader,
            folderPath: String,
            fileNames: List<String>
        ): List<T> {
            val data = mutableListOf<T>()

            for (fileName in fileNames) {
                val filePath = "$folderPath/$fileName"
                val fileBytes = resourceLoader.load(filePath)
                val templates = loadTemplates<T>(fileBytes)
                data += templates
            }

            return data
        }

        suspend inline fun <reified T : Template> create(
            resourceLoader: IResourceLoader,
            folderPath: String,
            fileNames: List<String>
        ): DataTable<T> {
            val data = mutableListOf<T>()

            for (fileName in fileNames) {
                val filePath = "$folderPath/$fileName"
                val fileBytes = resourceLoader.load(filePath)
                val templates = loadTemplates<T>(fileBytes)
                data += templates
            }

            return DataTable(data)
        }
    }

    fun find(id: String): T
    {
        return data[id] ?:
        throw IllegalArgumentException("No template found with id $id")
    }


    fun findOrNull(id: String): T?
    {
        return data[id]
    }

    fun has(id: String): Boolean
    {
        return data.containsKey(id)
    }

    fun all(): List<T>
    {
        return data.values.toList()
    }
}