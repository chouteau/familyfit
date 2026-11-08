using System.Globalization;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using QRCoder;

namespace FamilyFit.Web.Services;

public static class OperatorCommands
{
    public static async Task<bool> TryRunAsync(string[] args, IServiceProvider services, IWebHostEnvironment environment)
    {
        if (!args.Contains("--bootstrap") && !args.Contains("--recover") && !args.Contains("--backup")) return false;
        var configuration = services.GetRequiredService<IConfiguration>();
        if (configuration.GetValue<bool>("FamilyFit:Demo")) throw new InvalidOperationException("Les commandes d’exploitation ne s’exécutent pas sur la démonstration.");
        var data = configuration["FamilyFit:DataDirectory"]!;
        var factory = services.GetRequiredService<IDbContextFactory<FamilyDb>>();
        await using var db = await factory.CreateDbContextAsync();
        string? Option(string flag) { var i = Array.IndexOf(args, flag); return i >= 0 && i + 1 < args.Length ? args[i + 1] : null; }
        if (args.Contains("--backup"))
        {
            var sourcePath = new SqliteConnectionStringBuilder(db.Database.GetConnectionString()).DataSource;
            if (!File.Exists(sourcePath)) throw new InvalidOperationException("Aucune base existante à sauvegarder.");
            var directory = Path.Combine(data, "backups"); Directory.CreateDirectory(directory);
            var name = Option("--backup-name") ?? $"familyfit-{DateTimeOffset.UtcNow:yyyyMMdd-HHmmss}.db";
            if (Path.GetFileName(name) != name || !name.EndsWith(".db", StringComparison.Ordinal) || name.Contains("..")) throw new InvalidOperationException("Nom de sauvegarde invalide.");
            var target = Path.Combine(directory, name);
            if (File.Exists(target)) throw new InvalidOperationException("Cette sauvegarde existe déjà. Choisis un nouveau nom.");
            await using var source = new SqliteConnection(db.Database.GetConnectionString()); await source.OpenAsync();
            await using var destination = new SqliteConnection($"Data Source={target}"); await destination.OpenAsync();
            source.BackupDatabase(destination);
            Restrict(target);
            Console.WriteLine($"Sauvegarde SQLite créée : {target}");
            return true;
        }
        var admin = await db.Members.FirstOrDefaultAsync(x => x.IsAdmin);
        if (admin != null && !args.Contains("--recover")) throw new InvalidOperationException("Un administrateur existe déjà. Utilise --recover pour récupérer son accès.");
        if (admin == null)
        {
            var nickname = Option("--nickname") ?? "Administrateur";
            var age = int.TryParse(Option("--age"), out var suppliedAge) ? suppliedAge : 18;
            var weight = double.TryParse(Option("--weight"), NumberStyles.Float, CultureInfo.InvariantCulture, out var suppliedWeight) ? suppliedWeight : 70;
            var error = FamilyFit.Core.Validators.MemberValidator.Validate(new(nickname, age, weight, "À préciser dans mon profil", "Débutant", 30, "1,3,5", ""));
            if (error != null) throw new InvalidOperationException(error);
            admin = new Member { Nickname = nickname.Trim(), Age = age, WeightKg = weight, IsAdmin = true };
            db.Members.Add(admin); await db.SaveChangesAsync();
        }
        else
        {
            var now = services.GetRequiredService<TimeProvider>().GetUtcNow();
            await db.Devices.Where(x => x.MemberId == admin.Id && x.RevokedAtUtc == null).ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAtUtc, now));
            await db.BrowserSessions.Where(x => x.MemberId == admin.Id && x.RevokedAtUtc == null).ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAtUtc, now));
        }
        var grant = await services.GetRequiredService<AuthService>().CreateInvitationAsync(admin.Id);
        var privateDirectory = Path.Combine(data, "private"); Directory.CreateDirectory(privateDirectory);
        if (!OperatingSystem.IsWindows()) File.SetUnixFileMode(privateDirectory, UnixFileMode.UserRead | UnixFileMode.UserWrite | UnixFileMode.UserExecute);
        using var qr = QRCodeGenerator.GenerateQrCode(grant.QrPayload, QRCodeGenerator.ECCLevel.M);
        var path = Path.Combine(privateDirectory, $"bootstrap-{admin.Id}.html");
        var html = $"<!doctype html><html lang=\"fr\"><meta charset=\"utf-8\"><title>Activation FamilyFit</title><body><h1>Activer le téléphone administrateur</h1><p>Invitation personnelle, valable dix minutes. Ne pas partager.</p>{new SvgQRCode(qr).GetGraphic(6)}<p>Code : <strong>{grant.Code}</strong></p><p>Serveur : {System.Net.WebUtility.HtmlEncode(configuration["FamilyFit:PublicUrl"])}</p></body></html>";
        await File.WriteAllTextAsync(path, html); Restrict(path);
        Console.WriteLine($"Invitation créée dans le fichier privé : {path}. Récupère-le par SSH, active le mobile puis supprime la copie locale.");
        return true;
    }
    private static void Restrict(string file) { if (!OperatingSystem.IsWindows()) File.SetUnixFileMode(file, UnixFileMode.UserRead | UnixFileMode.UserWrite); }
}
