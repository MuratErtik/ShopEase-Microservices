import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { authService } from "../../services/authService";
import { useAuth } from "../../context/AuthContext";
import { useState } from "react";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [serverError, setServerError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { role: "USER" } });

  const onSubmit = async (data) => {
    setServerError("");
    try {
      await authService.register(data);
      const user = await login(data.email, data.password);
      navigate(user.role === "SELLER" ? "/seller" : "/");
    } catch (err) {
      setServerError(err.message || "Kayıt başarısız.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <Link to="/" className="font-semibold text-gray-900 text-xl">
            Vitrin
          </Link>
          <p className="mt-2 text-gray-500 text-sm">Yeni hesap oluştur</p>
        </div>

        <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Ad
                </label>
                <input
                  type="text"
                  placeholder="Ada"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  {...register("firstName", { required: "Ad zorunludur." })}
                />
                {errors.firstName && (
                  <p className="mt-1 text-xs text-red-500">
                    {errors.firstName.message}
                  </p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Soyad
                </label>
                <input
                  type="text"
                  placeholder="Yılmaz"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  {...register("lastName", { required: "Soyad zorunludur." })}
                />
                {errors.lastName && (
                  <p className="mt-1 text-xs text-red-500">
                    {errors.lastName.message}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                E-posta
              </label>
              <input
                type="email"
                placeholder="ornek@mail.com"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                {...register("email", {
                  required: "E-posta zorunludur.",
                  pattern: {
                    value: /\S+@\S+\.\S+/,
                    message: "Geçerli bir e-posta girin.",
                  },
                })}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-red-500">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Şifre
              </label>
              <input
                type="password"
                placeholder="••••••••"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                {...register("password", {
                  required: "Şifre zorunludur.",
                  minLength: { value: 8, message: "En az 8 karakter olmalı." },
                })}
              />
              {errors.password && (
                <p className="mt-1 text-xs text-red-500">
                  {errors.password.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Hesap türü
              </label>
              <div className="grid grid-cols-2 gap-3">
                <label className="flex items-center gap-2 border border-gray-300 rounded-lg px-3 py-2.5 cursor-pointer has-[checked]:border-gray-900 has-[checked]:bg-gray-50">
                  <input
                    type="radio"
                    value="USER"
                    {...register("role")}
                    className="accent-indigo-600"
                  />
                  <span className="text-sm text-gray-700">Alışveriş Yap</span>
                </label>
                <label className="flex items-center gap-2 border border-gray-300 rounded-lg px-3 py-2.5 cursor-pointer has-[checked]:border-gray-900 has-[checked]:bg-gray-50">
                  <input
                    type="radio"
                    value="SELLER"
                    {...register("role")}
                    className="accent-indigo-600"
                  />
                  <span className="text-sm text-gray-700">Satış Yap</span>
                </label>
              </div>
            </div>

            {serverError && (
              <p className="text-xs text-red-500 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {serverError}
              </p>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-indigo-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? "Kayıt olunuyor..." : "Kayıt Ol"}
            </button>
          </form>
        </div>

        <p className="mt-4 text-center text-sm text-gray-500">
          Zaten hesabın var mı?{" "}
          <Link
            to="/login"
            className="text-gray-900 font-medium hover:underline"
          >
            Giriş Yap
          </Link>
        </p>
      </div>
    </div>
  );
}
