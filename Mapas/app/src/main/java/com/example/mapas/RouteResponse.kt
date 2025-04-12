package com.example.mapas

data class RouteResponse(
    val features: List<Feature>
)

data class Feature(
    val geometry: Geometry
)

data class Geometry(
    val coordinates: List<List<Double>>,
    val type: String
)


data class GeocodeResponse(
    val features: List<GeocodeFeature>
)

data class GeocodeFeature(
    val geometry: GeocodeGeometry
)

data class GeocodeGeometry(
    val coordinates: List<Double>
)
