private void resolveGenerationAndSearch(
        CarSearchSpec car,
        int year,
        String fuel,
        String fuelName
) {

    if (car.exactModel != null &&
            !car.exactModel.trim().isEmpty()) {

        searchCarWithGeneration(
                car,
                car.exactModel,
                year,
                fuel,
                fuelName
        );

        return;
    }

    status.setText(
            "Намирам поколението за " +
            car.displayName +
            " " +
            year +
            " " +
            fuelName +
            "..."
    );

    new Thread(() -> {

        try {

            GenerationMatch match =
                    findGeneration(
                            car,
                            year,
                            fuel
                    );

            if (match == null ||
                    match.model == null ||
                    match.model.isEmpty()) {

                runOnUiThread(() ->
                        status.setText(
                                "Не намерих поколение с резултати за:\n" +
                                car.displayName +
                                " " +
                                year +
                                " " +
                                fuelName
                        )
                );

                return;
            }

            runOnUiThread(() -> {

                car.exactModel =
                        match.model;

                searchCarWithGeneration(
                        car,
                        match.model,
                        year,
                        fuel,
                        fuelName
                );
            });

        } catch (Exception e) {

            runOnUiThread(() ->
                    status.setText(
                            "Грешка при намиране на поколение: " +
                            e.getMessage()
                    )
            );
        }

    }).start();
}
