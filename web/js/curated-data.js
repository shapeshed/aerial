// Generated from app/src/main/java/com/shapeshed/aerial/data/RegistryRepository.kt
// (CURATED_MOOD_STATIONS) and app/src/main/res/values/strings.xml (mood_* labels).
// Keep this in sync by hand if the Kotlin source changes — there is no build step
// that regenerates it.

export const MOOD_GROUP_ORDER = ["relax", "focus", "morning", "driving", "late_night", "workout"];

export const MOOD_LABELS = {
  "relax": {
    "title": "Relax",
    "desc": "Unwind and let go"
  },
  "focus": {
    "title": "Focus",
    "desc": "Concentrate and be productive"
  },
  "morning": {
    "title": "Morning",
    "desc": "Start your day positively"
  },
  "driving": {
    "title": "On The Road",
    "desc": "A broad mix for the road"
  },
  "late_night": {
    "title": "Late night",
    "desc": "Music for the quiet hours"
  },
  "workout": {
    "title": "Workout",
    "desc": "High energy to keep going"
  }
};

// Each ref resolves to a registry station by (provider, providerId) when providerId
// is set, else by (name, provider), else by name alone — see resolveMoodStation()
// in curated.js, ported from RegistryRepository.kt's resolveMoodStation().
export const CURATED_MOOD_STATIONS = {
  "relax": [
    {
      "name": "Café del Mar",
      "provider": "radio-browser",
      "providerId": "3fd18c3f-8157-11e9-aa30-52543be04c81",
      "displayName": null
    },
    {
      "name": "ABC Lounge",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "nordic lodge copenhagen",
      "provider": "radio-browser",
      "providerId": "2ee81587-dba9-4d68-82b3-a7b32aafc525",
      "displayName": "Nordic Lodge Copenhagen"
    },
    {
      "name": "Bossa Jazz Brasil",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Radio Paradise Mellow Mix",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Skylab Radio",
      "provider": "radio-browser",
      "providerId": "24273571-703e-4373-b715-d7e7680d7599",
      "displayName": null
    },
    {
      "name": "OneLuvFM",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "FIP",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Jazz Sakura (asia dream radio)",
      "provider": "radio-browser",
      "providerId": "9766d47a-68a3-4a30-8aec-c026f1ec6020",
      "displayName": null
    },
    {
      "name": "Radio Samui Online",
      "provider": "radio-browser",
      "providerId": "86468748-3045-4b88-97ef-e6d266c901f5",
      "displayName": null
    }
  ],
  "focus": [
    {
      "name": "freeCodeCamp Code Radio",
      "provider": "radio-browser",
      "providerId": "60ede1ca-d7fa-4a36-a047-aec873b9be41",
      "displayName": null
    },
    {
      "name": "Slow Focus | NTS",
      "provider": "radio-browser",
      "providerId": "d5468df4-e6d0-11e9-a96c-52543be04c81",
      "displayName": null
    },
    {
      "name": "Sheet Music | NTS",
      "provider": "radio-browser",
      "providerId": "1e0ad463-0dbc-4913-b12d-7d0b7300882b",
      "displayName": null
    },
    {
      "name": "Systrum Sistum - SSR1",
      "provider": "radio-browser",
      "providerId": "37e6772a-5ab7-429d-84bc-fedc606cc8c4",
      "displayName": null
    },
    {
      "name": "SomaFM Beat Blender",
      "provider": "radio-browser",
      "providerId": "960eb232-0601-11e8-ae97-52543be04c81",
      "displayName": null
    },
    {
      "name": "A Strangely Isolated Place",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Box Lofi Radio",
      "provider": "radio-browser",
      "providerId": "a5213a32-d614-47bc-8d52-70a2b6eed8e1",
      "displayName": null
    },
    {
      "name": "SomaFM Groove Salad",
      "provider": "radio-browser",
      "providerId": "960cf833-0601-11e8-ae97-52543be04c81",
      "displayName": null
    },
    {
      "name": "FluxFM Chillhop – Chill Beats and LoFi HipHop",
      "provider": "radio-browser",
      "providerId": "58d3cce6-62b5-43f1-8f41-8d024998aabc",
      "displayName": null
    },
    {
      "name": "Radio Swiss Jazz",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    }
  ],
  "morning": [
    {
      "name": "Dogglounge",
      "provider": "radio-browser",
      "providerId": "1c41c07b-b995-11e8-aaf2-52543be04c81",
      "displayName": null
    },
    {
      "name": "D3EP Radio",
      "provider": "radio-browser",
      "providerId": "d210ac34-0fee-4586-b9d6-24b84e2c0c4e",
      "displayName": null
    },
    {
      "name": "Oroko Radio",
      "provider": "radio-browser",
      "providerId": "7babd377-ed7c-4a63-9778-47b0fd94983b",
      "displayName": null
    },
    {
      "name": "SomaFM Heavyweight Reggae",
      "provider": "radio-browser",
      "providerId": "c5955cee-2cdf-40b2-8b5f-aa55bafddbef",
      "displayName": null
    },
    {
      "name": "Pure Ibiza Radio",
      "provider": "radio-browser",
      "providerId": "26edc6b6-d221-4814-a6a9-0dd5d5d365d2",
      "displayName": null
    },
    {
      "name": "Veneno",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Cafe Mambo Ibiza Radio",
      "provider": "radio-browser",
      "providerId": "450a5177-1752-11ea-a620-52543be04c81",
      "displayName": null
    },
    {
      "name": "Radio Raheem",
      "provider": "radio-browser",
      "providerId": "a1c99f81-f8d1-4f6d-b9e3-714763a72b7d",
      "displayName": null
    },
    {
      "name": "Soho Radio",
      "provider": "radio-browser",
      "providerId": "b830f77b-cb54-431d-9bdb-0af9bc6af301",
      "displayName": null
    },
    {
      "name": "The Lot Radio",
      "provider": "radio-browser",
      "providerId": "434e9a4b-018a-4557-8ca1-8c328bb1e09d",
      "displayName": null
    }
  ],
  "driving": [
    {
      "name": "Radio alHara",
      "provider": "radio-browser",
      "providerId": "d62fa52b-7c1c-492c-9861-cd2c0ec02f00",
      "displayName": null
    },
    {
      "name": "Rinse FM",
      "provider": "rinse",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Refuge Worldwide",
      "provider": "radio-browser",
      "providerId": "edb81cbf-0645-4944-a376-554b8299ff27",
      "displayName": null
    },
    {
      "name": "Worldwide FM",
      "provider": "radio-browser",
      "providerId": "1651c32f-55d8-4429-995d-872ea0dcf520",
      "displayName": null
    },
    {
      "name": "ZonaSalsa Radio",
      "provider": "radio-browser",
      "providerId": "0957a107-50fc-435e-a402-ddfa5cdda777",
      "displayName": null
    },
    {
      "name": "Dandelion Radio",
      "provider": "radio-browser",
      "providerId": "961fe58c-0601-11e8-ae97-52543be04c81",
      "displayName": null
    },
    {
      "name": "Radio Paradise Main Mix",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "KEXP 90.3 Seattle",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Cashmere Radio",
      "provider": "radio-browser",
      "providerId": "356a337d-4552-4b2d-bfdb-d381a6d85a9d",
      "displayName": null
    },
    {
      "name": "Triple J",
      "provider": "abc",
      "providerId": "TRIPLEJ",
      "displayName": null
    }
  ],
  "late_night": [
    {
      "name": "SomaFM Drone Zone",
      "provider": "radio-browser",
      "providerId": "960eb2e9-0601-11e8-ae97-52543be04c81",
      "displayName": null
    },
    {
      "name": "Dinamo.fm Sleep",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "SomaFM - Deep Space One (128 kb/s AAC)",
      "provider": "radio-browser",
      "providerId": "0042e94c-55d6-4ff4-a3a7-46136e703424",
      "displayName": "SomaFM Deep Space One"
    },
    {
      "name": "Cryosleep",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "MyNoise Ocean Waves",
      "provider": "radio-browser",
      "providerId": "b69fe610-1522-47b1-a784-e7025d11f884",
      "displayName": null
    },
    {
      "name": "Nature Radio Rain",
      "provider": "radio-browser",
      "providerId": "95277bca-2c9a-4c08-b2e7-0854e5793f8e",
      "displayName": null
    },
    {
      "name": "Ambient Sleeping Pill | 128 kbps",
      "provider": "radio-browser",
      "providerId": "4b4d1308-9fdb-42a7-b92d-6370bc3284fe",
      "displayName": "Ambient Sleeping Pill"
    },
    {
      "name": "White Noise Radio",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Pink Noise Radio",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Brown Noise Radio",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    }
  ],
  "workout": [
    {
      "name": "BBC Radio 1 Dance (International)",
      "provider": "bbc",
      "providerId": "bbc_radio_one_dance_int",
      "displayName": "BBC Radio 1 Dance"
    },
    {
      "name": "Liquid DnB",
      "provider": "radio-browser",
      "providerId": "b8148b29-09d0-4aa1-8bfe-43d236260170",
      "displayName": null
    },
    {
      "name": "Bassdrive",
      "provider": "radio-browser",
      "providerId": "960cc332-0601-11e8-ae97-52543be04c81",
      "displayName": null
    },
    {
      "name": "Technolovers - TECHNO",
      "provider": "radio-browser",
      "providerId": "2100610c-13c2-4536-879f-6a88ccb07dc8",
      "displayName": null
    },
    {
      "name": "Kool FM",
      "provider": "rinse",
      "providerId": "kool",
      "displayName": null
    },
    {
      "name": "54house.fm",
      "provider": "radio-browser",
      "providerId": "a20e7f55-661e-4f4c-b87f-a087c64633f8",
      "displayName": null
    },
    {
      "name": "ShoutDRIVE",
      "provider": "radio-browser",
      "providerId": "b294bf46-7c1d-45ba-b852-71c5b26298ac",
      "displayName": null
    },
    {
      "name": "Techno.FM",
      "provider": "curated",
      "providerId": null,
      "displayName": null
    },
    {
      "name": "Radio FG 98.2",
      "provider": "radio-browser",
      "providerId": "4a1bbe28-0675-43bb-98dc-fae037b0b026",
      "displayName": null
    },
    {
      "name": "Hard Techno Radio",
      "provider": "radio-browser",
      "providerId": "6eef88eb-396c-4393-86aa-1ba04ff91918",
      "displayName": null
    }
  ]
};
