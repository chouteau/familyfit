# syntax=docker/dockerfile:1
FROM mcr.microsoft.com/dotnet/sdk:10.0.403-noble AS build
WORKDIR /source
COPY global.json NuGet.Config ./
COPY src/FamilyFit.Contracts/ src/FamilyFit.Contracts/
COPY src/FamilyFit.Core/ src/FamilyFit.Core/
COPY src/Web/ src/Web/
# Razor sources are present at restore time so Blazor/static assets are discovered correctly.
RUN dotnet restore src/Web/FamilyFit.Web.csproj
RUN dotnet publish src/Web/FamilyFit.Web.csproj -c Release -o /publish --no-restore /p:UseAppHost=false
RUN mkdir -p /data/media /data/backups && chmod 700 /data

FROM mcr.microsoft.com/dotnet/aspnet:10.0.14-noble-chiseled-extra AS runtime
WORKDIR /app
COPY --from=build --chown=app:app /data /data
COPY --from=build /publish ./
ENV ASPNETCORE_HTTP_PORTS=8080 \
    ASPNETCORE_ENVIRONMENT=Production \
    FamilyFit__DataDirectory=/data \
    DOTNET_EnableDiagnostics=0
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD ["dotnet", "FamilyFit.Web.dll", "--healthcheck"]
ENTRYPOINT ["dotnet", "FamilyFit.Web.dll"]
