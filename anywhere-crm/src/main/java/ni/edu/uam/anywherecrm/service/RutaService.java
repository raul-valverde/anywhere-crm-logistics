package ni.edu.uam.anywherecrm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Calcula la ruta del camión consumiendo dos APIs públicas y gratuitas de OpenStreetMap:
//  1. Nominatim: convierte la dirección del cliente en coordenadas (latitud y longitud)
//  2. OSRM: calcula la ruta en carro desde la bodega hasta esas coordenadas
public class RutaService {

    // Punto de salida de los camiones. Cambiar por la ubicación real de la bodega.
    public static final double BODEGA_LATITUD = 12.143899;
    public static final double BODEGA_LONGITUD = -86.254877;

    private static final String URL_NOMINATIM = "https://nominatim.openstreetmap.org/search";
    private static final String URL_OSRM = "https://router.project-osrm.org/route/v1/driving/";
    // Nominatim pide que cada aplicación se identifique con un User-Agent
    private static final String USER_AGENT = "AnywhereCRM/1.0 (proyecto academico)";

    private final ObjectMapper json = new ObjectMapper();

    // Resultado de la API: destino, distancia y tiempo
    public static class Ruta {
        private final String destino;
        private final double latitud;
        private final double longitud;
        private final double kilometros;
        private final double minutos;

        public Ruta(String destino, double latitud, double longitud, double kilometros, double minutos) {
            this.destino = destino;
            this.latitud = latitud;
            this.longitud = longitud;
            this.kilometros = kilometros;
            this.minutos = minutos;
        }

        public String getDestino() { return destino; }
        public double getKilometros() { return kilometros; }
        public double getMinutos() { return minutos; }

        // Texto corto que se guarda en el despacho, por ejemplo: "2.4 km, 5 min aprox."
        public String resumen() {
            return String.format("%.1f km, %.0f min aprox.", kilometros, minutos);
        }

        // Enlace para ver la ruta dibujada en el mapa de OpenStreetMap
        public String enlaceMapa() {
            return "https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route="
                    + BODEGA_LATITUD + "%2C" + BODEGA_LONGITUD + "%3B" + latitud + "%2C" + longitud;
        }
    }

    // Busca la dirección y calcula la ruta desde la bodega
    public Ruta calcularRuta(String direccion) throws IOException {
        // 1. Dirección -> coordenadas (Nominatim)
        String consulta = URLEncoder.encode(direccion, StandardCharsets.UTF_8);
        JsonNode lugares = consultar(URL_NOMINATIM + "?format=json&limit=1&countrycodes=ni&q=" + consulta);
        if (lugares.isEmpty()) {
            throw new IOException("No se encontró \"" + direccion + "\". Escribe un lugar conocido: barrio, colonia, centro comercial o ciudad.");
        }
        JsonNode lugar = lugares.get(0);
        double latitud = lugar.path("lat").asDouble();
        double longitud = lugar.path("lon").asDouble();
        String destino = lugar.path("display_name").asText();

        // 2. Ruta en carro bodega -> destino (OSRM). OSRM pide longitud,latitud
        JsonNode respuesta = consultar(URL_OSRM + BODEGA_LONGITUD + "," + BODEGA_LATITUD + ";"
                + longitud + "," + latitud + "?overview=false");
        if (!"Ok".equals(respuesta.path("code").asText()) || respuesta.path("routes").isEmpty()) {
            throw new IOException("No se encontró una ruta en carro hasta esa dirección");
        }
        JsonNode ruta = respuesta.path("routes").get(0);
        double kilometros = ruta.path("distance").asDouble() / 1000;  // viene en metros
        double minutos = ruta.path("duration").asDouble() / 60;       // viene en segundos

        return new Ruta(destino, latitud, longitud, kilometros, minutos);
    }

    // Hace la petición GET y convierte la respuesta JSON en un árbol que se puede recorrer
    private JsonNode consultar(String url) throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conexion.setRequestMethod("GET");
        conexion.setRequestProperty("User-Agent", USER_AGENT);
        conexion.setConnectTimeout(10000); // 10 segundos para conectar
        conexion.setReadTimeout(15000);    // 15 segundos para responder
        try {
            int codigo = conexion.getResponseCode();
            if (codigo != 200) {
                throw new IOException("El servicio de mapas respondió con error " + codigo);
            }
            try (InputStream cuerpo = conexion.getInputStream()) {
                return json.readTree(cuerpo);
            }
        } finally {
            conexion.disconnect();
        }
    }
}