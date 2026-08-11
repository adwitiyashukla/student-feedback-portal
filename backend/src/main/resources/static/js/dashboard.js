(function () {
    "use strict";

    const source = document.getElementById("chart-data");
    if (!source || typeof Chart === "undefined") {
        return;
    }

    function split(value) {
        return (value || "").split("|").filter(function (part) {
            return part.length > 0;
        });
    }

    const trendRows = split(source.dataset.trend).map(function (row) {
        const parts = row.split(":");
        return { period: parts[0], submitted: Number(parts[1]), resolved: Number(parts[2]) };
    });

    const trendCanvas = document.getElementById("trendChart");
    if (trendCanvas && trendRows.length) {
        new Chart(trendCanvas, {
            type: "line",
            data: {
                labels: trendRows.map(function (r) { return r.period; }),
                datasets: [
                    {
                        label: "Submitted",
                        data: trendRows.map(function (r) { return r.submitted; }),
                        borderColor: "#4f46e5",
                        backgroundColor: "rgba(79, 70, 229, .12)",
                        fill: true,
                        tension: 0.3
                    },
                    {
                        label: "Resolved",
                        data: trendRows.map(function (r) { return r.resolved; }),
                        borderColor: "#10b981",
                        backgroundColor: "rgba(16, 185, 129, .12)",
                        fill: true,
                        tension: 0.3
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                resizeDelay: 100,
                interaction: { mode: "index", intersect: false },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });
    }

    const categories = split(source.dataset.categories);
    const counts = split(source.dataset.categoryCounts).map(Number);

    const categoryCanvas = document.getElementById("categoryChart");
    if (categoryCanvas && categories.length) {
        new Chart(categoryCanvas, {
            type: "doughnut",
            data: {
                labels: categories.map(function (name) {
                    return name.replace(/_/g, " ");
                }),
                datasets: [{
                    data: counts,
                    backgroundColor: [
                        "#4f46e5", "#0ea5e9", "#10b981", "#f59e0b", "#ef4444",
                        "#8b5cf6", "#14b8a6", "#f97316", "#64748b", "#ec4899"
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                resizeDelay: 100,
                plugins: {
                    legend: { position: "right", labels: { boxWidth: 12, font: { size: 11 } } }
                }
            }
        });
    }
})();
