package com

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

// ─── Tablas Exposed ───────────────────────────────────────────────────────────

object Usuarios : Table("usuarios") {
    val id         = integer("id").autoIncrement()
    val nombre     = varchar("nombre", 100)
    val correo     = varchar("correo", 100)
    val passwordHash = varchar("password_hash", 255)
    val role       = varchar("role", 20)
    val fotoUrl    = varchar("foto_url", 255).nullable()
    val createdAt  = timestamp("created_at").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Sectores : Table("sectores") {
    val id     = integer("id").autoIncrement()
    val nombre = varchar("nombre", 100)
    override val primaryKey = PrimaryKey(id)
}

object Carreras : Table("carreras") {
    val id       = integer("id").autoIncrement()
    val idSector = integer("id_sector").references(Sectores.id)
    val nombre   = varchar("nombre", 100)
    override val primaryKey = PrimaryKey(id)
}

object Tarjetas : Table("tarjetas") {
    val id        = integer("id").autoIncrement()
    val idCarrera = integer("id_carrera").references(Carreras.id)
    val texto     = varchar("texto", 255)
    val imagenUrl = varchar("imagen_url", 255).nullable()
    val activa    = bool("activa").default(true)
    override val primaryKey = PrimaryKey(id)
}

object TestProgress : Table("test_progress") {
    val id               = integer("id").autoIncrement()
    val idUsuario        = integer("id_usuario").references(Usuarios.id)
    val ultimaTarjeta    = integer("ultima_tarjeta").default(0)
    val totalCompletadas = integer("total_completadas").default(0)
    val finalizado       = bool("finalizado").default(false)
    override val primaryKey = PrimaryKey(id)
}

object RespuestasIndividuales : Table("respuestas_individuales") {
    val id         = integer("id").autoIncrement()
    val idUsuario  = integer("id_usuario").references(Usuarios.id)
    val idTarjeta  = integer("id_tarjeta").references(Tarjetas.id)
    val leIntereso = bool("le_intereso")
    val fecha      = timestamp("fecha").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Resultados : Table("resultados") {
    val id             = integer("id").autoIncrement()
    val idUsuario      = integer("id_usuario").references(Usuarios.id)
    val sectorSugerido = varchar("sector_sugerido", 100)
    val carrerasAfines = text("carreras_afines")
    val fecha          = timestamp("fecha").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}

object Universidades : Table("universidades") {
    val id               = integer("id").autoIncrement()
    val nombre           = varchar("nombre", 150)
    val localidad        = varchar("localidad", 150).nullable()
    val sitioWeb         = varchar("sitio_web", 255).nullable()
    val ofertaEducativa  = text("oferta_educativa").nullable()
    val logoUrl          = varchar("logo_url", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}

object UniversidadSector : Table("universidad_sector") {
    val idUniversidad = integer("id_universidad").references(Universidades.id)
    val idSector      = integer("id_sector").references(Sectores.id)
    override val primaryKey = PrimaryKey(idUniversidad, idSector)
}

// ─── Configuración de conexión ────────────────────────────────────────────────

fun Application.configureExposed() {
    val host     = System.getenv("MYSQLHOST")           ?: "localhost"
    val port     = System.getenv("MYSQLPORT")           ?: "3306"
    val database = System.getenv("MYSQL_DATABASE")      ?: "matchvoc"
    val user     = System.getenv("MYSQLUSER")           ?: "root"
    val password = System.getenv("MYSQL_ROOT_PASSWORD") ?: "root"

    Database.connect(
        url      = "jdbc:mysql://$host:$port/$database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
        driver   = "com.mysql.cj.jdbc.Driver",
        user     = user,
        password = password
    )
}