package my.company.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import my.company.ai.data.model.Project

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val rootPath: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain(): Project = Project(
        id = id,
        name = name,
        packageName = packageName,
        rootPath = rootPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(project: Project): ProjectEntity = ProjectEntity(
            id = project.id,
            name = project.name,
            packageName = project.packageName,
            rootPath = project.rootPath,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
        )
    }
}
