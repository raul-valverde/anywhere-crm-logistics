package ni.edu.uam.anywherecrm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Consume dos APIs públicas y gratuitas de OpenStreetMap (no necesitan clave):
//  1. Nominatim: convierte una dirección en coordenadas (latitud y longitud)
//  2. OSRM "trip": recibe varias paradas y devuelve el MEJOR ORDEN para recorrerlas
//     saliendo de la bodega y regresando a ella, con la distancia y el tiempo total
public class RutaService {

    // Punto de salida y regreso de los camiones. Cambiar por la ubicación real de la bodega.
    public static final double BODEGA_LATITUD = 12.1364;
    public static final double BODEGA_LONGITUD = -86.2514;

    private static final String URL_NOMINATIM = "https://nominatim.openstreetmap.org/search";
    private static final String URL_OSRM_TRIP = "https://router.project-osrm.org/trip/v1/driving/";
    // Nominatim pide que cada aplicación se identifique con un User-Agent
    private static final String USER_AGENT = "AnywhereCRM/1.0 (proyecto academico)";

    private final ObjectMapper json = new ObjectMapper();

    // Resultado de buscar una dirección
    public static class Ubicacion {
        private final double latitud;
        private final double longitud;
        private final String nombre;

        public Ubicacion(double latitud, double longitud, String nombre) {
            this.latitud = latitud;
            this.longitud = longitud;
            this.nombre = nombre;
        }

        public double getLatitud() { return latitud; }
        public double getLongitud() { return longitud; }
        public String getNombre() { return nombre; }
    }

    // Resultado de planificar un viaje con varias paradas
    public static class PlanRuta {
        private final List<Integer> orden; // Posiciones de las paradas recibidas, en el orden en que se visitan
        private final double kilometros;
        private final double minutos;
        private final String enlaceMapa;

        public PlanRuta(List<Integer> orden, double kilometros, double minutos, String enlaceMapa) {
            this.orden = orden;
            this.kilometros = kilometros;
            this.minutos = minutos;
            this.enlaceMapa = enlaceMapa;
        }

        public List<Integer> getOrden() { return orden; }
        public double getKilometros() { return kilometros; }
        public double getMinutos() { return minutos; }
        public String getEnlaceMapa() { return enlaceMapa; }

        public String resumen() {
            return String.format("%d paradas, %.1f km, %.0f min aprox. (ida y vuelta)", orden.size(), kilometros, minutos);
        }
    }

    // 1. Dirección -> coordenadas (Nominatim)
    public Ubicacion ubicar(String direccion) throws IOException {
        String consulta = URLEncoder.encode(direccion, StandardCharsets.UTF_8);
        JsonNode lugares = consultar(URL_NOMINATIM + "?format=json&limit=1&countrycodes=ni&q=" + consulta);
        if (lugares.isEmpty()) {
            throw new IOException("No se encontró \"" + direccion + "\". Escribe un lugar conocido: barrio, colonia, centro comercial o ciudad.");
        }
        JsonNode lugar = lugares.get(0);
        return new Ubicacion(lugar.path("lat").asDouble(), lugar.path("lon").asDouble(), lugar.path("display_name").asText());
    }

    // 2. Varias paradas -> mejor orden, distancia y tiempo (OSRM trip)
    // Cada parada es {latitud, longitud}
    public PlanRuta planificar(List<double[]> paradas) throws IOException {
        if (paradas.isEmpty()) {
            throw new IOException("La ruta no tiene paradas");
        }

        // OSRM pide "longitud,latitud" separados por ";". La primera es la bodega.
        StringBuilder coordenadas = new StringBuilder(BODEGA_LONGITUD + "," + BODEGA_LATITUD);
        for (double[] parada : paradas) {
            coordenadas.append(";").append(parada[1]).append(",").append(parada[0]);
        }
        // source=first: sale de la bodega. roundtrip=true: regresa a la bodega.
        JsonNode respuesta = consultar(URL_OSRM_TRIP + coordenadas + "?source=first&roundtrip=true&overview=false");
        if (!"Ok".equals(respuesta.path("code").asText()) || respuesta.path("trips").isEmpty()) {
            throw new IOException("No se encontró una ruta en carro entre esas direcciones");
        }

        // waypoints viene en el mismo orden en que mandamos los puntos.
        // waypoint_index dice en qué lugar del viaje queda cada uno (la bodega es el 0).
        JsonNode puntos = respuesta.path("waypoints");
        Integer[] visitaEnPosicion = new Integer[paradas.size()];
        for (int i = 0; i < paradas.size(); i++) {
            int posicionEnViaje = puntos.get(i + 1).path("waypoint_index").asInt(); // +1 porque el 0 es la bodega
            visitaEnPosicion[posicionEnViaje - 1] = i;
        }
        List<Integer> orden = new ArrayList<>(List.of(visitaEnPosicion));

        JsonNode viaje = respuesta.path("trips").get(0);
        double kilometros = viaje.path("distance").asDouble() / 1000; // viene en metros
        double minutos = viaje.path("duration").asDouble() / 60;      // viene en segundos

        return new PlanRuta(orden, kilometros, minutos, enlaceGoogleMaps(paradas, orden));
    }

    // Google Maps dibuja el recorrido: bodega -> paradas en orden -> bodega
    private String enlaceGoogleMaps(List<double[]> paradas, List<Integer> orden) {
        StringBuilder enlace = new StringBuilder("https://www.google.com/maps/dir/" + BODEGA_LATITUD + "," + BODEGA_LONGITUD);
        for (int indice : orden) {
            enlace.append("/").append(paradas.get(indice)[0]).append(",").append(paradas.get(indice)[1]);
        }
        enlace.append("/").append(BODEGA_LATITUD).append(",").append(BODEGA_LONGITUD);
        return enlace.toString();
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